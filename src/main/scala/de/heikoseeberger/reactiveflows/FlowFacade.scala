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

import akka.actor.{ Actor, ActorContext, ActorLogging, ActorRef, Props }
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator.{ Changed, Subscribe }
import akka.cluster.ddata.{ LWWMap, LWWMapKey, Replicator }
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

object FlowFacade {

  type CreateFlow = (ActorContext, String, ActorRef) => ActorRef

  final case class FlowDesc(name: String, label: String)

  sealed trait FlowEvent

  final case object GetFlows
  final case class Flows(flows: Set[FlowDesc])

  final case class AddFlow(label: String)
  final case class FlowExists(desc: FlowDesc)
  final case class FlowAdded(desc: FlowDesc) extends FlowEvent

  final case class RemoveFlow(name: String)
  final case class FlowUnknown(name: String)
  final case class FlowRemoved(name: String) extends FlowEvent

  final case class GetMessages(name: String, id: Long, count: Short)

  final case class AddMessage(name: String, text: String)

  final val Name = "flow-facade"

  val flows = LWWMapKey[FlowDesc]("flows")

  private val updateFlowData =
    Replicator.Update(flows, LWWMap.empty[FlowDesc], Replicator.WriteLocal) _

  def apply(mediator: ActorRef,
            replicator: ActorRef,
            flowShardRegion: ActorRef,
            createFlow: CreateFlow = createFlow): Props =
    Props(new FlowFacade(mediator, replicator, flowShardRegion, createFlow))

  private def createFlow(context: ActorContext, name: String, mediator: ActorRef) =
    context.actorOf(Flow(mediator), name)

  private def labelToName(label: String) =
    URLEncoder.encode(label.toLowerCase, UTF_8.name)
}

final class FlowFacade(mediator: ActorRef,
                       replicator: ActorRef,
                       flowShardRegion: ActorRef,
                       createFlow: FlowFacade.CreateFlow)
    extends Actor
    with ActorLogging {
  import FlowFacade._

  private implicit val cluster = Cluster(context.system)

  private var flowsByName = Map.empty[String, FlowDesc]

  replicator ! Subscribe(flows, self)

  override def receive = {
    case GetFlows => sender() ! Flows(flowsByName.valuesIterator.to[Set])

    case AddFlow("") => badCommand("label empty")
    case af: AddFlow => handleAddFlow(af)

    case RemoveFlow("") => badCommand("name empty")
    case rf: RemoveFlow => handleRemoveFlow(rf)

    case GetMessages("", _, _) => badCommand("name empty")
    case gm: GetMessages       => handleGetMessages(gm)

    case AddMessage("", _) => badCommand("name empty")
    case am: AddMessage    => handleAddMessage(am)

    case c @ Changed(`flows`) => flowsByName = c.get(flows).entries
  }

  protected def forwardToFlow(name: String, message: Any): Unit =
    flowShardRegion.forward(name -> message)

  private def handleAddFlow(addFlow: AddFlow) = {
    import addFlow._
    withUnknownFlow(label) { name =>
      val desc = FlowDesc(name, label)
      flowsByName += name -> desc
      replicator ! updateFlowData(_ + (name -> desc))
      val flowAdded = FlowAdded(desc)
      mediator ! Publish(className[FlowEvent], flowAdded)
      log.info("Flow with name '{}' added", name)
      sender() ! flowAdded
    }
  }

  private def handleRemoveFlow(removeFlow: RemoveFlow) = {
    import removeFlow._
    withExistingFlow(name) {
      flowsByName -= name
      replicator ! updateFlowData(_ - name)
      forwardToFlow(name, Flow.Stop)
      val flowRemoved = FlowRemoved(name)
      mediator ! Publish(className[FlowEvent], flowRemoved)
      log.info("Flow with name '{}' removed", name)
      sender() ! flowRemoved
    }
  }

  private def handleGetMessages(getMessages: GetMessages) = {
    import getMessages._
    withExistingFlow(name) {
      forwardToFlow(name, Flow.GetMessages(id, count))
    }
  }

  private def handleAddMessage(addMessage: AddMessage) = {
    import addMessage._
    withExistingFlow(name) {
      forwardToFlow(name, Flow.AddMessage(text))
    }
  }

  private def withUnknownFlow(label: String)(f: String => Unit) = {
    val name = labelToName(label)
    flowsByName.get(name).fold(f(name))(desc => sender() ! FlowExists(desc))
  }

  private def withExistingFlow(name: String)(action: => Unit) =
    if (flowsByName.contains(name)) action else sender() ! FlowUnknown(name)
}
