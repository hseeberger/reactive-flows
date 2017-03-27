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

import akka.actor.{ ActorLogging, Props }
import akka.persistence.PersistentActor
import java.io.{ Serializable => JavaSerializable }
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

object Flows {

  sealed trait Command
  sealed trait Event
  sealed trait Serializable extends JavaSerializable

  // == Message protocol – start ==

  final case class AddFlow(label: String)     extends Serializable
  final case class FlowAdded(desc: FlowDesc)  extends Serializable with Event
  final case class FlowExists(desc: FlowDesc) extends Serializable

  final case class RemoveFlow(name: String)  extends Serializable
  final case class FlowRemoved(name: String) extends Serializable with Event
  final case class FlowUnknown(name: String) extends Serializable

  // == Message protocol – nested objects ==

  final case class FlowDesc(name: String, label: String) extends Serializable

  // == Message protocol – end ==

  final val Name = "flow-desc-repository"

  def apply(): Props =
    Props(new Flows)

  private def labelToName(label: String) = URLEncoder.encode(label.toLowerCase, UTF_8.name)
}

final class Flows extends PersistentActor with ActorLogging {
  import Flows._

  override val persistenceId = Name

  private var flowsByName = Map.empty[String, FlowDesc]

  override def receiveCommand = {
    case AddFlow(label)   => handleAddFlow(label)
    case RemoveFlow(name) => handleRemoveFlow(name)
  }

  override def receiveRecover = {
    case event: Event => handleEvent(event)
    // TODO Use snapshots!
  }

  private def handleEvent(event: Event) =
    event match {
      case FlowAdded(desc)   => flowsByName += desc.name -> desc
      case FlowRemoved(name) => flowsByName -= name
    }

  private def handleAddFlow(label: String) = {
    val name = labelToName(label)
    def add() =
      persist(FlowAdded(FlowDesc(name, label))) { flowAdded =>
        handleEvent(flowAdded)
        log.info("Flow with name '{}' added", name)
        sender() ! flowAdded
      }
    flowsByName.get(name) match {
      case None       => add()
      case Some(desc) => sender() ! FlowExists(desc)
    }
  }

  private def handleRemoveFlow(name: String) =
    if (flowsByName.contains(name)) {
      persist(FlowRemoved(name)) { flowRemoved =>
        handleEvent(flowRemoved)
        log.info("Flow with name '{}' removed", name)
        sender() ! flowRemoved
      }
    } else
      sender() ! FlowUnknown(name)
}
