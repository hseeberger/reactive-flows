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

import akka.actor.{ ActorLogging, ActorRef, ActorSystem, Props }
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.sharding.ShardRegion.Passivate
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import akka.persistence.PersistentActor
import java.time.LocalDateTime
import scala.math.{ max, min }

object Flow {

  sealed trait MessageEvent

  // == Message protocol – start ==

  final case class GetMessages(id: Long, count: Short)
  final case class Messages(messages: Vector[Message])

  final case class AddMessage(text: String)
  final case class MessageAdded(name: String, message: Message) extends MessageEvent

  final case object Stop
  // No response

  private final case object Terminate
  // No response

  // == Message protocol – end ==

  final case class Message(id: Long, text: String, time: LocalDateTime)

  def apply(mediator: ActorRef): Props =
    Props(new Flow(mediator))

  def startSharding(system: ActorSystem, mediator: ActorRef, shardCount: Int): ActorRef = {
    def shardId(name: String) = (name.hashCode.abs % shardCount).toString
    ClusterSharding(system).start(className[Flow],
                                  Flow(mediator),
                                  ClusterShardingSettings(system),
                                  { case (name: String, msg) => (name, msg) },
                                  { case (name: String, _)   => shardId(name) })
  }
}

final class Flow(mediator: ActorRef) extends PersistentActor with ActorLogging {
  import Flow._

  override val persistenceId = s"flow-${self.path.name}"

  private var messages = Vector.empty[Message]

  override def receiveCommand = {
    case GetMessages(id, _) if id < 0        => badCommand("id < 0")
    case GetMessages(_, count) if count <= 0 => badCommand("count <= 0")
    case GetMessages(id, count)              => handleGetMessages(id, count)

    case AddMessage("")   => badCommand("text empty")
    case AddMessage(text) => handleAddMessage(text)

    case Stop      => context.parent ! Passivate(Terminate)
    case Terminate => context.stop(self)
  }

  override def receiveRecover = {
    case event: MessageEvent => handleEvent(event)
  }

  private def handleGetMessages(id: Long, count: Short) = {
    // We can use proper `Long` values in a later step!
    val intId = min(id, Int.MaxValue).toInt
    val n     = max(messages.size - 1 - intId, 0)
    sender() ! Messages(messages.slice(n, n + count))
  }

  private def handleAddMessage(text: String) = {
    val message = Message(messages.size, text, LocalDateTime.now())
    persist(MessageAdded(self.path.name, message)) { messageAdded =>
      handleEvent(messageAdded)
      mediator ! Publish(className[MessageEvent], messageAdded)
      log.info("Message starting with '{}' added", text.take(42))
      sender() ! messageAdded
    }
  }

  private def handleEvent(event: MessageEvent) =
    event match {
      case MessageAdded(_, message) => messages +:= message
    }
}
