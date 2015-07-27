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
import java.net.URLEncoder

object FlowFacade {

  case object GetFlows
  case class FlowDescriptor(name: String, label: String)

  case class AddFlow(label: String)
  case class FlowAdded(flowDescriptor: FlowDescriptor)
  case class FlowExists(label: String)

  case class RemoveFlow(name: String)
  case class FlowRemoved(name: String)
  case class FlowUnknown(name: String)

  // $COVERAGE-OFF$
  final val Name = "flow-facade"
  // $COVERAGE-ON$

  def props: Props = Props(new FlowFacade)

  private def labelToName(label: String) = URLEncoder.encode(label.toLowerCase, "UTF-8")
}

class FlowFacade extends Actor {
  import FlowFacade._

  private var flowsByName = Map.empty[String, FlowDescriptor]

  override def receive = {
    case GetFlows                                                    => sender() ! flowsByName.valuesIterator.to[Set]
    case AddFlow(label) if !flowsByName.contains(labelToName(label)) => addFlow(label)
    case AddFlow(label)                                              => sender() ! FlowExists(label)
    case RemoveFlow(name) if flowsByName.contains(name)              => removeFlow(name)
    case RemoveFlow(name)                                            => sender() ! FlowUnknown(name)
  }

  private def addFlow(label: String) = {
    val name = labelToName(label)
    val flowDescriptor = FlowDescriptor(name, label)
    flowsByName += name -> flowDescriptor
    sender() ! FlowAdded(flowDescriptor)
  }

  private def removeFlow(name: String) = {
    flowsByName -= name
    sender() ! FlowRemoved(name)
  }
}
