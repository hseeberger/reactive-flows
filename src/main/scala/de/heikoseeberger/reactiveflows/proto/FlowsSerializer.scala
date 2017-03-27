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
import de.heikoseeberger.reactiveflows.proto.flowfacade.{
  AddFlow => AddFlowPb,
  AddPost => AddPostPb,
  FlowAdded => FlowAddedPb,
  FlowDesc => FlowDescPb,
  FlowExists => FlowExistsPb,
  FlowRemoved => FlowRemovedPb,
  FlowUnknown => FlowUnknownPb,
  Flows => FlowsPb,
  GetFlows => GetFlowsPb,
  GetPosts => GetPostsPb,
  RemoveFlow => RemoveFlowPb
}
import java.io.NotSerializableException
import scala.collection.breakOut

final class FlowsSerializer extends SerializerWithStringManifest {
  import Flows._

  override val identifier = getClass.getName.hashCode // Good idea?

  private final val AddFlowManifest     = "AddFlow"
  private final val FlowAddedManifest   = "FlowAdded"
  private final val FlowExistsManifest  = "FlowExists"
  private final val RemoveFlowManifest  = "RemoveFlow"
  private final val FlowRemovedManifest = "FlowRemoved"
  private final val FlowUnknownManifest = "FlowUnknown"
  private final val FlowDescManifest    = "FlowDesc"

  override def manifest(o: AnyRef) =
    o match {
      case serializable: Serializable =>
        serializable match {
          case _: AddFlow     => AddFlowManifest
          case _: FlowAdded   => FlowAddedManifest
          case _: FlowExists  => FlowExistsManifest
          case _: RemoveFlow  => RemoveFlowManifest
          case _: FlowRemoved => FlowRemovedManifest
          case _: FlowUnknown => FlowUnknownManifest
          case _: FlowDesc    => FlowDescManifest
        }
      case _ => throw new IllegalArgumentException(s"Unknown class: ${o.getClass}!")
    }

  override def toBinary(o: AnyRef) = {
    def flowDescPb(d: FlowDesc) = FlowDescPb(d.name, d.label)
    val pb =
      o match {
        case serializable: Serializable =>
          serializable match {
            case AddFlow(label)        => AddFlowPb(label)
            case FlowAdded(desc)       => FlowAddedPb(Some(flowDescPb(desc)))
            case FlowExists(desc)      => FlowExistsPb(Some(flowDescPb(desc)))
            case RemoveFlow(name)      => RemoveFlowPb(name)
            case FlowRemoved(name)     => FlowRemovedPb(name)
            case FlowUnknown(name)     => FlowUnknownPb(name)
            case FlowDesc(name, label) => FlowDescPb(name, label)
          }
        case _ => throw new IllegalArgumentException(s"Unknown class: ${o.getClass}!")
      }
    pb.toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String) = {
    def addFlow(pb: AddFlowPb)         = AddFlow(pb.label)
    def flowAdded(pb: FlowAddedPb)     = FlowAdded(flowDesc(pb.desc.get))
    def flowExists(pb: FlowExistsPb)   = FlowExists(flowDesc(pb.desc.get))
    def removeFlow(pb: RemoveFlowPb)   = RemoveFlow(pb.name)
    def flowRemoved(pb: FlowRemovedPb) = FlowRemoved(pb.name)
    def flowUnknown(pb: FlowUnknownPb) = FlowUnknown(pb.name)
    def flowDesc(pb: FlowDescPb)       = FlowDesc(pb.name, pb.label)
    manifest match {
      case AddFlowManifest     => addFlow(AddFlowPb.parseFrom(bytes))
      case FlowAddedManifest   => flowAdded(FlowAddedPb.parseFrom(bytes))
      case FlowExistsManifest  => flowExists(FlowExistsPb.parseFrom(bytes))
      case RemoveFlowManifest  => removeFlow(RemoveFlowPb.parseFrom(bytes))
      case FlowRemovedManifest => flowRemoved(FlowRemovedPb.parseFrom(bytes))
      case FlowUnknownManifest => flowUnknown(FlowUnknownPb.parseFrom(bytes))
      case FlowDescManifest    => flowDesc(FlowDescPb.parseFrom(bytes))
      case _                   => throw new NotSerializableException(manifest)
    }
  }
}
