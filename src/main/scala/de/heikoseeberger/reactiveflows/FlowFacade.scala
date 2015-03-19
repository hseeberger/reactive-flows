/*
 * Copyright 2015 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.reactiveflows

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.cluster.Cluster
import akka.cluster.ddata.{ LWWMap, LWWMapKey, Replicator }
import akka.cluster.pubsub.DistributedPubSubMediator
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import java.net.URLEncoder

object FlowFacade {

  case object GetFlows
  case class FlowDescriptor(name: String, label: String)

  sealed trait FlowEvent

  case class AddFlow(label: String)
  case class FlowAdded(flowDescriptor: FlowDescriptor) extends FlowEvent
  case class ExistingFlow(label: String)

  case class RemoveFlow(name: String)
  case class FlowRemoved(name: String) extends FlowEvent
  case class UnknownFlow(name: String)

  case class GetMessages(flowName: String)
  case class AddMessage(flowName: String, text: String)

  final val Name = "flow-facade"

  final val FlowEventKey = "flow-events"

  private val replicatorKey = LWWMapKey[FlowDescriptor]("flows")

  private val replicatorUpdate = Replicator.Update(replicatorKey, LWWMap.empty[FlowDescriptor], Replicator.WriteLocal) _

  private final val TypeName = "flow"

  def props(mediator: ActorRef, replicator: ActorRef): Props = Props(new FlowFacade(mediator, replicator))

  def startSharding(system: ActorSystem, mediator: ActorRef, shardCount: Int): Unit =
    ClusterSharding(system).start(
      TypeName,
      Flow.props(mediator),
      ClusterShardingSettings(system),
      extractEntityId,
      extractShardId(shardCount)
    )

  private def extractEntityId: ShardRegion.ExtractEntityId = {
    case (name: String, payload) => (name, payload)
  }

  private def extractShardId(shardCount: Int): ShardRegion.ExtractShardId = {
    case (name: String, _) => (name.hashCode % shardCount).toString
  }

  private def labelToName(label: String) = URLEncoder.encode(label.toLowerCase, "UTF-8")
}

class FlowFacade(mediator: ActorRef, replicator: ActorRef) extends Actor with ActorLogging {
  import FlowFacade._

  private implicit val cluster = Cluster(context.system)

  private val shardRegion = flowShardRegion()

  private var flowDescriptorByName = Map.empty[String, FlowDescriptor]

  replicator ! Replicator.Subscribe(replicatorKey, self)

  override def receive = {
    case GetFlows                                      => sender() ! flowDescriptorByName.valuesIterator.toSet
    case AddFlow(label)                                => addFlow(label)
    case RemoveFlow(name)                              => removeFlow(name)
    case GetMessages(flowName)                         => getMessages(flowName)
    case AddMessage(flowName, text)                    => addMessage(flowName, text)
    case changed @ Replicator.Changed(`replicatorKey`) => flowDescriptorByName = changed.get(replicatorKey).entries
  }

  protected def forwardToFlow(name: String)(message: Any): Unit = shardRegion.forward(name -> message)

  protected def flowShardRegion(): ActorRef = ClusterSharding(context.system).shardRegion(TypeName)

  private def addFlow(label: String) = withUnknownFlow(label) { name =>
    val flowDescriptor = FlowDescriptor(name, label)
    val flowAdded = FlowAdded(flowDescriptor)
    flowDescriptorByName += name -> flowDescriptor
    mediator ! DistributedPubSubMediator.Publish(FlowEventKey, flowAdded)
    replicator ! replicatorUpdate(_ + (name -> flowDescriptor))
    sender() ! flowAdded
  }

  private def removeFlow(name: String) = withExistingFlow(name) { name =>
    forwardToFlow(name)(Flow.Stop)
    flowDescriptorByName -= name
    replicator ! replicatorUpdate(_ - name)
    val flowRemoved = FlowRemoved(name)
    mediator ! DistributedPubSubMediator.Publish(FlowEventKey, flowRemoved)
    sender() ! flowRemoved
  }

  private def getMessages(flowName: String) = withExistingFlow(flowName) { name =>
    forwardToFlow(name)(Flow.GetMessages)
  }

  private def addMessage(flowName: String, text: String) = withExistingFlow(flowName) { name =>
    forwardToFlow(name)(Flow.AddMessage(text))
  }

  private def withExistingFlow(name: String)(f: String => Unit) =
    if (flowDescriptorByName.contains(name))
      f(name)
    else
      sender() ! UnknownFlow(name)

  private def withUnknownFlow(label: String)(f: String => Unit) = {
    val name = labelToName(label)
    if (!flowDescriptorByName.contains(name))
      f(name)
    else
      sender() ! ExistingFlow(label)
  }
}
