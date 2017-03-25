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

  sealed trait Command
  sealed trait Event
  sealed trait Serializable extends JavaSerializable

  // == Message protocol – start ==

  final case class GetPosts(seqNo: Long, count: Int) extends Serializable with Command
  final case class Posts(posts: Vector[Post])        extends Serializable

  final case class AddPost(text: String)               extends Serializable with Command
  final case class PostAdded(name: String, post: Post) extends Serializable with Event

  final case object Stop extends Serializable with Command
  // No response

  final case class CommandEnvelope(name: String, command: Command) extends Serializable

  private final case object Terminate
  // No response

  // == Message protocol – nested objects

  final case class Post(seqNo: Long, text: String, time: Instant)

  // == Message protocol – end ==

  def apply(mediator: ActorRef): Props =
    Props(new Flow(mediator))

  def startSharding(system: ActorSystem, mediator: ActorRef, shardCount: Int): ActorRef = {
    def shardId(name: String) = (name.hashCode.abs % shardCount).toString
    ClusterSharding(system).start(
      className[Flow],
      Flow(mediator),
      ClusterShardingSettings(system),
      { case CommandEnvelope(name, command) => (name, command) },
      { case CommandEnvelope(name, _)       => shardId(name) }
    )
  }
}

final class Flow(mediator: ActorRef) extends PersistentActor with ActorLogging {
  import Flow._

  override val persistenceId = s"flow-${self.path.name}"

  private var posts = Vector.empty[Post]

  override def receiveCommand = {
    case GetPosts(seqNo, _) if seqNo < 0  => badCommand("seqNo < 0")
    case GetPosts(_, count) if count <= 0 => badCommand("count <= 0")
    case GetPosts(seqNo, count)           => handleGetPosts(seqNo, count)

    case AddPost("")   => badCommand("text empty")
    case AddPost(text) => handleAddPost(text)

    case Stop      => context.parent ! Passivate(Terminate) // TODO Is parent really ShardRegion?
    case Terminate => context.stop(self)
  }

  override def receiveRecover = {
    case event: Event => handleEvent(event)
    // TODO Use Snapshots!
  }

  private def handleGetPosts(seqNo: Long, count: Int) = {
    // TODO: We can use proper `Long` values in a later step!
    val seqNoInt = min(seqNo, Int.MaxValue).toInt
    val n        = max(posts.size - 1 - seqNoInt, 0)
    sender() ! Posts(posts.slice(n, n + count))
  }

  private def handleAddPost(text: String) = {
    val post = Post(posts.size, text, Instant.now())
    persist(PostAdded(self.path.name, post)) { postAdded =>
      handleEvent(postAdded)
      mediator ! Publish(className[Event], postAdded)
      log.info("Post starting with '{}' added", text.take(42))
      sender() ! postAdded
    }
  }

  private def handleEvent(event: Event) =
    event match {
      case PostAdded(_, post) => posts +:= post
    }
}
