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

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.cluster.pubsub.DistributedPubSubMediator
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import akka.persistence.PersistentActor
import java.time.LocalDateTime

object Flow {

  sealed trait MessageEvent

  case object GetMessages
  case class Message(text: String, time: LocalDateTime)

  case class AddMessage(text: String)
  case class MessageAdded(flowName: String, message: Message) extends MessageEvent

  case object Stop

  // $COVERAGE-OFF$
  def startSharding(system: ActorSystem, mediator: ActorRef, shardCount: Int): Unit = ClusterSharding(system).start(
    className[Flow],
    props(mediator),
    ClusterShardingSettings(system),
    { case (name: String, payload) => (name, payload) },
    { case (name: String, _) => (name.hashCode % shardCount).toString }
  )
  // $COVERAGE-ON$

  def props(mediator: ActorRef): Props = Props(new Flow(mediator))
}

class Flow(mediator: ActorRef) extends PersistentActor {
  import Flow._

  private val name = self.path.name

  private var messages = Vector.empty[Message]

  override def persistenceId = s"flow-$name"

  override def receiveCommand = {
    case GetMessages      => sender() ! messages
    case AddMessage(text) => addMessage(text)
    case Stop             => context.stop(self)
  }

  override def receiveRecover = {
    case MessageAdded(_, message) => messages +:= message
  }

  private def addMessage(text: String) = persist(MessageAdded(name, Message(text, LocalDateTime.now()))) { messageAdded =>
    receiveRecover(messageAdded)
    mediator ! DistributedPubSubMediator.Publish(className[MessageEvent], messageAdded)
    sender() ! messageAdded
  }
}
