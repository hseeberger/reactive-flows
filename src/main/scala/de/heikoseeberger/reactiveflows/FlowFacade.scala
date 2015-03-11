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

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
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

  def props(mediator: ActorRef) = Props(new FlowFacade(mediator))

  private def labelToName(label: String) = URLEncoder.encode(label.toLowerCase, "UTF-8")
}

class FlowFacade(mediator: ActorRef) extends Actor with ActorLogging {

  import FlowFacade._

  private var flowInfoByName = Map.empty[String, FlowInfo]

  override def receive = {
    case AddFlow(label)             => addFlow(label)
    case RemoveFlow(name)           => removeFlow(name)
    case GetFlows                   => sender() ! flowInfoByName.valuesIterator.toSet
    case AddMessage(flowName, text) => addMessage(flowName, text)
    case GetMessages(flowName)      => getMessages(flowName)
  }

  protected def createFlow(name: String): ActorRef = context.actorOf(Flow.props(mediator), name)

  protected def forwardToFlow(name: String)(message: Any): Unit = context.child(name).foreach(_.forward(message))

  private def addFlow(label: String) = {
    val name = labelToName(label)
    if (flowInfoByName.contains(name))
      sender() ! FlowExists(label)
    else {
      createFlow(name)
      val flowInfo = FlowInfo(name, label)
      val flowAdded = FlowAdded(flowInfo)
      flowInfoByName += name -> flowInfo
      mediator ! PubSubMediator.Publish(FlowEventKey, flowAdded)
      sender() ! flowAdded
    }
  }

  private def removeFlow(name: String) = withKnownFlow(name) {
    context.child(name).foreach(context.stop)
    flowInfoByName -= name
    val flowRemoved = FlowRemoved(name)
    mediator ! PubSubMediator.Publish(FlowEventKey, flowRemoved)
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
