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

import de.heikoseeberger.akkasse.ServerSentEvent
import spray.json.{ PrettyPrinter, jsonWriter }

object ServerSentEventProtocol extends ServerSentEventProtocol

trait ServerSentEventProtocol {

  import JsonProtocol._

  implicit def flowEventToServerSentEvent(event: FlowFacade.FlowEvent): ServerSentEvent = event match {
    case FlowFacade.FlowAdded(flow) =>
      val data = PrettyPrinter(jsonWriter[FlowFacade.FlowInfo].write(flow))
      ServerSentEvent(data, "added")
    case FlowFacade.FlowRemoved(name) =>
      ServerSentEvent(name, "removed")
  }

  implicit def messageEventToServerSentEvent(event: Flow.MessageEvent): ServerSentEvent = event match {
    case messageAdded: Flow.MessageAdded =>
      val data = PrettyPrinter(jsonWriter[Flow.MessageAdded].write(messageAdded))
      ServerSentEvent(data, "added")
  }
}
