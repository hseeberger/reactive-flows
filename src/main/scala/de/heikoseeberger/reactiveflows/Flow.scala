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
import de.heikoseeberger.reactiveflows.PubSubMediator.Publish
import java.time.LocalDateTime
import scala.math.{ max, min }

object Flow {

  final case class Message(id: Long, text: String, time: LocalDateTime)

  sealed trait MessageEvent

  final case class GetMessages(id: Long, count: Short)
  final case class Messages(messages: Vector[Message])

  final case class AddMessage(text: String)
  final case class MessageAdded(name: String, message: Message)
      extends MessageEvent

  def apply(mediator: ActorRef): Props =
    Props(new Flow(mediator))
}

final class Flow(mediator: ActorRef) extends Actor with ActorLogging {
  import Flow._

  private var messages = Vector.empty[Message]

  override def receive = {
    case GetMessages(id, _) if id < 0        => badCommand("id < 0")
    case GetMessages(_, count) if count <= 0 => badCommand("count <= 0")
    case gm: GetMessages                     => handleGetMessages(gm)

    case AddMessage("") => badCommand("text empty")
    case am: AddMessage => handleAddMessage(am)
  }

  private def handleGetMessages(getMessages: GetMessages) = {
    import getMessages._
    // We can use proper `Long` values in a later step!
    val intId = min(id, Int.MaxValue).toInt
    val n     = max(messages.size - 1 - intId, 0)
    sender() ! Messages(messages.slice(n, n + count))
  }

  private def handleAddMessage(addMessage: AddMessage) = {
    import addMessage._
    val message = Message(messages.size, text, LocalDateTime.now())
    messages +:= message
    val messageAdded = MessageAdded(self.path.name, message)
    mediator ! Publish(className[MessageEvent], messageAdded)
    log.info("Message starting with '{}' added", text.take(42))
    sender() ! messageAdded
  }
}
