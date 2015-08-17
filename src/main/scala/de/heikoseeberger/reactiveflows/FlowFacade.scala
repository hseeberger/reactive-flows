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

import akka.actor.{ Actor, ActorRef, Props }
import java.net.URLEncoder

object FlowFacade {

  case object GetFlows
  case class FlowDescriptor(name: String, label: String)

  case class AddFlow(label: String)
  case class FlowAdded(flowDescriptor: FlowDescriptor)
  case class FlowExists(label: String)

  case class RemoveFlow(name: String)
  case class FlowRemoved(name: String)
  case class FlowUnknown(name: String)

  case class GetMessages(flowName: String)
  case class AddMessage(flowName: String, text: String)

  // $COVERAGE-OFF$
  final val Name = "flow-facade"
  // $COVERAGE-ON$

  def props: Props = Props(new FlowFacade)

  private def labelToName(label: String) = URLEncoder.encode(label.toLowerCase, "UTF-8")
}

class FlowFacade extends Actor {
  import FlowFacade._

  private var flowsByName = Map.empty[String, FlowDescriptor]

  override def receive = {
    case GetFlows                   => sender() ! flowsByName.valuesIterator.to[Set]
    case AddFlow(label)             => addFlow(label)
    case RemoveFlow(name)           => removeFlow(name)
    case GetMessages(flowName)      => getMessages(flowName)
    case AddMessage(flowName, text) => addMessage(flowName, text)
  }

  protected def createFlow(name: String): ActorRef = context.actorOf(Flow.props, name)

  protected def forwardToFlow(name: String)(message: Any): Unit = context.child(name).foreach(_.forward(message))

  private def addFlow(label: String) = withUnknownFlow(label) { name =>
    val flowDescriptor = FlowDescriptor(name, label)
    flowsByName += name -> flowDescriptor
    createFlow(name)
    sender() ! FlowAdded(flowDescriptor)
  }

  private def removeFlow(name: String) = withExistingFlow(name) { name =>
    flowsByName -= name
    sender() ! FlowRemoved(name)
  }

  private def getMessages(flowName: String) = withExistingFlow(flowName) { name =>
    forwardToFlow(name)(Flow.GetMessages)
  }

  private def addMessage(flowName: String, text: String) = withExistingFlow(flowName) { name =>
    forwardToFlow(name)(Flow.AddMessage(text))
  }

  private def withUnknownFlow(label: String)(f: String => Unit) = {
    val name = labelToName(label)
    if (!flowsByName.contains(name)) f(name) else sender() ! FlowExists(label)
  }

  private def withExistingFlow(name: String)(f: String => Unit) =
    if (flowsByName.contains(name)) f(name) else sender() ! FlowUnknown(name)
}
