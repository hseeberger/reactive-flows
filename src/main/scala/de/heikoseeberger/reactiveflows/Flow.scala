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

import akka.actor.{ ActorRef, Props }
import akka.contrib.pattern.DistributedPubSubMediator
import akka.persistence.PersistentActor
import java.time.LocalDateTime

object Flow {

  case object GetMessages

  sealed trait MessageEvent

  case class AddMessage(text: String)
  case class MessageAdded(flowName: String, message: Message) extends MessageEvent

  case class Message(text: String, dateTime: LocalDateTime)

  case object Stop

  final val MessageEventKey = "message-events"

  def props(mediator: ActorRef) = Props(new Flow(mediator))
}

class Flow(mediator: ActorRef) extends PersistentActor {

  import Flow._

  private val name = self.path.name

  private var messages = List.empty[Message]

  override def persistenceId = s"flow-$name"

  override def receiveCommand = {
    case GetMessages      => sender() ! messages
    case AddMessage(text) => addMessage(text)
    case Stop             => context.stop(self)
  }

  override def receiveRecover = {
    case messageAdded: MessageAdded => updateState(messageAdded)
  }

  private def addMessage(text: String) = persist(MessageAdded(name, Message(text, LocalDateTime.now()))) { messageAdded =>
    updateState(messageAdded)
    mediator ! DistributedPubSubMediator.Publish(MessageEventKey, messageAdded)
    sender() ! messageAdded
  }

  private def updateState(event: MessageEvent) = event match {
    case MessageAdded(flowName, message) => messages +:= message
  }
}
