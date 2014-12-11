/*
 * Copyright 2014 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.reactiveflows

import akka.actor.{ ActorLogging, Props }
import akka.contrib.pattern.{ DistributedPubSubExtension, DistributedPubSubMediator }
import akka.persistence.PersistentActor
import java.time.LocalDateTime
import spray.json.DefaultJsonProtocol

object Flow {

  sealed trait Event

  case object GetMessages

  case class AddMessage(text: String)
  case class MessageAdded(flowName: String, message: Message) extends Event
  object MessageAdded extends DefaultJsonProtocol { implicit val format = jsonFormat2(apply) }

  val EventKey = "flow-events"

  def props: Props =
    Props(new Flow)
}

class Flow
    extends PersistentActor
    with ActorLogging {

  import Flow._

  private val name = self.path.name

  private val mediator = DistributedPubSubExtension(context.system).mediator

  private var messages = List.empty[Message]

  log.debug("Flow [{}] created", name)

  override def persistenceId: String =
    s"flow-$name"

  // Command handling

  override def receiveCommand: Receive = {
    case GetMessages      => getMessages()
    case AddMessage(text) => addMessage(text)
  }

  private def getMessages(): Unit =
    sender() ! messages

  private def addMessage(text: String): Unit =
    persist(MessageAdded(name, Message(text, LocalDateTime.now()))) { messageAdded =>
      updateState(messageAdded)
      mediator ! DistributedPubSubMediator.Publish(EventKey, messageAdded)
      sender() ! messageAdded
    }

  // Event handling

  override def receiveRecover: Receive = {
    case messageAdded: MessageAdded => updateState(messageAdded)
  }

  private def updateState(event: Event): Unit = {
    def onMessageAdded(flowName: String, message: Message) = {
      messages +:= message
      log.info("Added message [{}] to flow [{}]", message, flowName)
    }
    event match {
      case MessageAdded(flowName, message) => onMessageAdded(flowName, message)
    }
  }
}
