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

import akka.actor.{ Actor, Props }
import java.time.LocalDateTime

object Flow {

  case object GetMessages

  sealed trait MessageEvent

  case class AddMessage(text: String)
  case class MessageAdded(flowName: String, message: Message) extends MessageEvent

  case class Message(text: String, dateTime: LocalDateTime)

  def props = Props(new Flow)
}

class Flow extends Actor {

  import Flow._

  private var messages = List.empty[Message]

  override def receive = {
    case GetMessages      => sender() ! messages
    case AddMessage(text) => addMessage(text)
  }

  private def addMessage(text: String) = {
    val message = Message(text, LocalDateTime.now())
    messages +:= message
    val messageAdded = MessageAdded(self.path.name, message)
    sender() ! messageAdded
  }
}
