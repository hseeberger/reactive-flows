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

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import spray.json.{ DefaultJsonProtocol, JsValue, JsString, RootJsonFormat }

object JsonProtocol extends JsonProtocol

trait JsonProtocol extends DefaultJsonProtocol {

  implicit object LocalDateTimeFormat extends RootJsonFormat[LocalDateTime] {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override def write(localDateTime: LocalDateTime) = JsString(formatter.format(localDateTime))

    override def read(json: JsValue) = json match {
      case JsString(s) => LocalDateTime.from(formatter.parse(s))
      case _           => throw new IllegalArgumentException(s"JsString expected: $json")
    }
  }

  implicit val flowInfoFormat = jsonFormat2(FlowFacade.FlowInfo)

  implicit val flowAddedFormat = jsonFormat1(FlowFacade.FlowAdded)

  implicit val flowExistsFormat = jsonFormat1(FlowFacade.FlowExists)

  implicit val flowRemovedFormat = jsonFormat1(FlowFacade.FlowRemoved)

  implicit val unknownFlowFormat = jsonFormat1(FlowFacade.UnknownFlow)

  implicit val messageFormat = jsonFormat2(Flow.Message)

  implicit val messageAddedFormat = jsonFormat2(Flow.MessageAdded)

  implicit val addFlowRequestFormat = jsonFormat1(HttpService.AddFlowRequest)

  implicit val addMessageRequestFormat = jsonFormat1(HttpService.AddMessageRequest)
}
