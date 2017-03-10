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
  AddMessage => AddMessagePb,
  Command => CommandPb,
  Envelope => EnvelopePb,
  GetMessages => GetMessagesPb,
  Instant => InstantPb,
  Message => MessagePb,
  MessageAdded => MessageAddedPb,
  Messages => MessagesPb,
  Stop => StopPb
}
import java.io.NotSerializableException
import java.time.Instant
import scala.collection.breakOut

final class FlowSerializer extends SerializerWithStringManifest {
  import Flow._

  override val identifier = getClass.getName.hashCode // Good idea?

  private final val GetMessagesManifest  = "GetMessages"
  private final val MessagesManifest     = "Messages"
  private final val AddMessageManifest   = "AddMessage"
  private final val MessageAddedManifest = "MessageAdded"
  private final val EnvelopeManifest     = "Envelope"
  private final val StopManifest         = "Stop"

  override def manifest(o: AnyRef) =
    o match {
      case _: GetMessages  => GetMessagesManifest
      case _: Messages     => MessagesManifest
      case _: AddMessage   => AddMessageManifest
      case _: MessageAdded => MessageAddedManifest
      case Stop            => StopManifest
      case _: Envelope     => EnvelopeManifest
      case _               => throw new IllegalArgumentException(s"Unknown class: ${o.getClass}!")
    }

  override def toBinary(o: AnyRef) = {
    def instantPb(i: Instant) = InstantPb(i.getEpochSecond, i.getNano)
    def messagePb(m: Message) = MessagePb(m.id, m.text, Some(instantPb(m.time)))
    def envelope(name: String, command: Command) = {
      val cmdPb = {
        command match {
          case GetMessages(id, count) => CmdPb.GetMessages(GetMessagesPb(id, count))
          case AddMessage(text)       => CmdPb.AddMessage(AddMessagePb(text))
          case Stop                   => CmdPb.Stop(StopPb())
        }
      }
      EnvelopePb(name, Some(CommandPb(cmdPb)))
    }
    val pb =
      o match {
        case GetMessages(id, count)      => GetMessagesPb(id, count)
        case Messages(messages)          => MessagesPb(messages.map(messagePb))
        case AddMessage(text)            => AddMessagePb(text)
        case MessageAdded(name, message) => MessageAddedPb(name, Some(messagePb(message)))
        case Stop                        => StopPb()
        case Envelope(n, command)        => envelope(n, command)
        case _                           => throw new IllegalArgumentException(s"Unknown class: ${o.getClass}!")
      }
    pb.toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String) = {
    def getMessages(pb: GetMessagesPb)   = GetMessages(pb.id, pb.count)
    def messages(pb: MessagesPb)         = Messages(pb.messages.map(message)(breakOut))
    def addMessage(pb: AddMessagePb)     = AddMessage(pb.text)
    def messageAdded(pb: MessageAddedPb) = MessageAdded(pb.name, message(pb.message.get))
    def envelope(pb: EnvelopePb) = {
      def command(cmdPb: CmdPb) =
        cmdPb match {
          case CmdPb.GetMessages(pb) => getMessages(pb)
          case CmdPb.AddMessage(pb)  => addMessage(pb)
          case CmdPb.Stop(_)         => Stop
          case _                     => throw new NotSerializableException("command must not be empty!")
        }
      Envelope(pb.name, command(pb.command.get.command))
    }
    def message(pb: MessagePb) = Message(pb.id, pb.text, instant(pb.time.get))
    def instant(pb: InstantPb) = Instant.ofEpochSecond(pb.epochSecond, pb.nano)
    manifest match {
      case GetMessagesManifest  => getMessages(GetMessagesPb.parseFrom(bytes))
      case MessagesManifest     => messages(MessagesPb.parseFrom(bytes))
      case AddMessageManifest   => addMessage(AddMessagePb.parseFrom(bytes))
      case MessageAddedManifest => messageAdded(MessageAddedPb.parseFrom(bytes))
      case StopManifest         => Stop
      case EnvelopeManifest     => envelope(EnvelopePb.parseFrom(bytes))
      case _                    => throw new NotSerializableException(manifest)
    }
  }
}
