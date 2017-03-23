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
import java.io.{ Serializable => JavaSerializable }
import java.time.Instant
import scala.math.{ max, min }

object Flow {

  sealed trait SerializableMessage extends JavaSerializable
  sealed trait Command             extends SerializableMessage
  sealed trait Event

  // == Message protocol – start ==

  final case class GetMessages(id: Long, count: Int)   extends Command
  final case class Messages(messages: Vector[Message]) extends SerializableMessage

  final case class AddMessage(text: String) extends Command
  final case class MessageAdded(name: String, message: Message)
      extends SerializableMessage
      with Event

  final case object Stop extends Command
  // No response

  final case class Envelope(name: String, command: Command) extends SerializableMessage // for sharding

  private final case object Terminate
  // No response

  final case class Message(id: Long, text: String, time: Instant)

  // == Message protocol – end ==

  def apply(mediator: ActorRef): Props =
    Props(new Flow(mediator))

  def startSharding(system: ActorSystem, mediator: ActorRef, shardCount: Int): ActorRef = {
    def shardId(name: String) = (name.hashCode.abs % shardCount).toString
    ClusterSharding(system).start(className[Flow],
                                  Flow(mediator),
                                  ClusterShardingSettings(system),
                                  { case Envelope(name, command) => (name, command) },
                                  { case Envelope(name, _)       => shardId(name) })
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

    case Stop      => context.parent ! Passivate(Terminate) // parent is **local** shard region
    case Terminate => context.stop(self)
  }

  override def receiveRecover = {
    case event: Event => handleEvent(event)
  }

  private def handleGetMessages(id: Long, count: Int) = {
    // TODO: We can use proper `Long` values in a later step!
    val intId = min(id, Int.MaxValue).toInt
    val until = min(intId.toLong + count, Int.MaxValue).toInt
    sender() ! Messages(messages.slice(intId, until))
  }

  private def handleAddMessage(text: String) = {
    val message = Message(messages.size, text, Instant.now())
    persist(MessageAdded(self.path.name, message)) { messageAdded =>
      handleEvent(messageAdded)
      mediator ! Publish(className[Event], messageAdded)
      log.info("Message starting with '{}' added", text.take(42))
      sender() ! messageAdded
    }
  }

  private def handleEvent(event: Event) =
    event match {
      case MessageAdded(_, message) => messages :+= message
    }
}
