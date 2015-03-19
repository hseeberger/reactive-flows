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
import akka.contrib.datareplication.{ LWWMap, Replicator }
import akka.contrib.pattern.{ ClusterSharding, DistributedPubSubMediator, ShardRegion }
import java.net.URLEncoder

object FlowFacade {

  case object GetFlows

  sealed trait FlowEvent

  case class AddFlow(label: String)
  case class RemoveFlow(name: String)

  case class FlowAdded(flowInfo: FlowInfo) extends FlowEvent
  case class FlowRemoved(name: String) extends FlowEvent

  case class AddMessage(flowName: String, text: String)
  case class GetMessages(flowName: String)

  case class FlowExists(label: String)
  case class UnknownFlow(name: String)

  case class FlowInfo(name: String, label: String)

  final val Name = "flow-facade"

  final val FlowEventKey = "flow-events"

  private final val ReplicatorKey = "flows"

  private final val TypeName = "flow"

  def props(mediator: ActorRef, replicator: ActorRef) = Props(new FlowFacade(mediator, replicator))

  def startSharding(system: ActorSystem, mediator: ActorRef, shardCount: Int): Unit =
    ClusterSharding(system).start(
      TypeName,
      Some(Flow.props(mediator)),
      idExtractor,
      shardResolver(shardCount)
    )

  private def idExtractor: ShardRegion.IdExtractor = {
    case (name: String, payload) => (name, payload)
  }

  private def shardResolver(shardCount: Int): ShardRegion.ShardResolver = {
    case (name: String, _) => (name.hashCode % shardCount).toString
  }

  private def labelToName(label: String) = URLEncoder.encode(label.toLowerCase, "UTF-8")

  private val replicatorUpdate =
    Replicator.Update(ReplicatorKey, LWWMap.empty[FlowInfo], Replicator.ReadLocal, Replicator.WriteLocal) _
}

class FlowFacade(mediator: ActorRef, replicator: ActorRef) extends Actor with ActorLogging {

  import FlowFacade._

  private implicit val cluster = Cluster(context.system)

  private val shardRegion = flowShardRegion()

  private var flowInfoByName = Map.empty[String, FlowInfo]

  replicator ! Replicator.Subscribe(ReplicatorKey, self)

  override def receive = {
    case AddFlow(label)                          => addFlow(label)
    case RemoveFlow(name)                        => removeFlow(name)
    case GetFlows                                => sender() ! flowInfoByName.valuesIterator.toSet
    case AddMessage(flowName, text)              => addMessage(flowName, text)
    case GetMessages(flowName)                   => getMessages(flowName)
    case Replicator.Changed(ReplicatorKey, data) => flowInfoByName = data.asInstanceOf[LWWMap[FlowInfo]].entries
  }

  protected def forwardToFlow(name: String)(message: Any): Unit = shardRegion.forward(name -> message)

  protected def flowShardRegion(): ActorRef = ClusterSharding(context.system).shardRegion(TypeName)

  private def addFlow(label: String) = {
    val name = labelToName(label)
    if (flowInfoByName.contains(name))
      sender() ! FlowExists(label)
    else {
      val flowInfo = FlowInfo(name, label)
      val flowAdded = FlowAdded(flowInfo)
      flowInfoByName += name -> flowInfo
      replicator ! replicatorUpdate(_ + (name -> flowInfo))
      mediator ! DistributedPubSubMediator.Publish(FlowEventKey, flowAdded)
      sender() ! flowAdded
    }
  }

  private def removeFlow(name: String) = withKnownFlow(name) {
    forwardToFlow(name)(Flow.Stop)
    flowInfoByName -= name
    replicator ! replicatorUpdate(_ - name)
    val flowRemoved = FlowRemoved(name)
    mediator ! DistributedPubSubMediator.Publish(FlowEventKey, flowRemoved)
    sender() ! flowRemoved
  }

  private def addMessage(flowName: String, text: String) = withKnownFlow(flowName) {
    forwardToFlow(flowName)(Flow.AddMessage(text))
  }

  private def getMessages(flowName: String) = withKnownFlow(flowName) {
    forwardToFlow(flowName)(Flow.GetMessages)
  }

  private def withKnownFlow(name: String)(effect: => Unit) =
    if (flowInfoByName.contains(name))
      effect
    else
      sender() ! UnknownFlow(name)
}
