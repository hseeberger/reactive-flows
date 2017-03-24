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
package proto

import akka.serialization.SerializerWithStringManifest
import de.heikoseeberger.reactiveflows.proto.flow.Command.{ Command => CmdPb }
import de.heikoseeberger.reactiveflows.proto.flow.{
  AddPost => AddPostPb,
  Command => CommandPb,
  Envelope => EnvelopePb,
  GetPosts => GetPostsPb,
  Instant => InstantPb,
  Post => PostPb,
  PostAdded => PostAddedPb,
  Posts => PostsPb,
  Stop => StopPb
}
import java.io.NotSerializableException
import java.time.Instant
import scala.collection.breakOut

final class FlowSerializer extends SerializerWithStringManifest {
  import Flow._

  override val identifier = getClass.getName.hashCode // Good idea?

  private final val GetPostsManifest        = "GetPosts"
  private final val PostsManifest           = "Posts"
  private final val AddPostManifest         = "AddPost"
  private final val PostAddedManifest       = "PostAdded"
  private final val CommandEnvelopeManifest = "CommandEnvelope"
  private final val StopManifest            = "Stop"

  override def manifest(o: AnyRef) =
    o match {
      case serializable: Serializable =>
        serializable match {
          case _: GetPosts        => GetPostsManifest
          case _: Posts           => PostsManifest
          case _: AddPost         => AddPostManifest
          case _: PostAdded       => PostAddedManifest
          case Stop               => StopManifest
          case _: CommandEnvelope => CommandEnvelopeManifest
        }
      case _ => throw new IllegalArgumentException(s"Unknown class: ${o.getClass}!")
    }

  override def toBinary(o: AnyRef) = {
    def envelope(name: String, command: Command) = {
      val cmdPb = {
        command match {
          case GetPosts(from, count) => CmdPb.GetPosts(GetPostsPb(from, count))
          case AddPost(text)         => CmdPb.AddPost(AddPostPb(text))
          case Stop                  => CmdPb.Stop(StopPb())
        }
      }
      EnvelopePb(name, Some(CommandPb(cmdPb)))
    }
    def postPb(m: Post)       = PostPb(m.id, m.text, Some(instantPb(m.time)))
    def instantPb(i: Instant) = InstantPb(i.getEpochSecond, i.getNano)
    val pb =
      o match {
        case serializable: Serializable =>
          serializable match {
            case GetPosts(from, count)          => GetPostsPb(from, count)
            case Posts(posts)                   => PostsPb(posts.map(postPb))
            case AddPost(text)                  => AddPostPb(text)
            case PostAdded(name, post)          => PostAddedPb(name, Some(postPb(post)))
            case Stop                           => StopPb()
            case CommandEnvelope(name, command) => envelope(name, command)
          }
        case _ => throw new IllegalArgumentException(s"Unknown class: ${o.getClass}!")
      }
    pb.toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String) = {
    def getPosts(pb: GetPostsPb)   = GetPosts(pb.from, pb.count)
    def posts(pb: PostsPb)         = Posts(pb.posts.map(post)(breakOut))
    def addPost(pb: AddPostPb)     = AddPost(pb.text)
    def postAdded(pb: PostAddedPb) = PostAdded(pb.name, post(pb.post.get))
    def envelope(pb: EnvelopePb) = {
      def command(cmdPb: CmdPb) =
        cmdPb match {
          case CmdPb.GetPosts(pb) => getPosts(pb)
          case CmdPb.AddPost(pb)  => addPost(pb)
          case CmdPb.Stop(_)      => Stop
          case CmdPb.Empty        => throw new NotSerializableException("command must not be empty!")
        }
      CommandEnvelope(pb.name, command(pb.command.get.command))
    }
    def post(pb: PostPb)       = Post(pb.id, pb.text, instant(pb.time.get))
    def instant(pb: InstantPb) = Instant.ofEpochSecond(pb.epochSecond, pb.nano)
    manifest match {
      case GetPostsManifest        => getPosts(GetPostsPb.parseFrom(bytes))
      case PostsManifest           => posts(PostsPb.parseFrom(bytes))
      case AddPostManifest         => addPost(AddPostPb.parseFrom(bytes))
      case PostAddedManifest       => postAdded(PostAddedPb.parseFrom(bytes))
      case StopManifest            => Stop
      case CommandEnvelopeManifest => envelope(EnvelopePb.parseFrom(bytes))
      case _                       => throw new NotSerializableException(manifest)
    }
  }
}
