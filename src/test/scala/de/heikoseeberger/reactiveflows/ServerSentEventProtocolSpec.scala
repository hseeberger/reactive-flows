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
import org.scalatest.{ Matchers, WordSpec }
import spray.json.pimpString

class ServerSentEventProtocolSpec extends WordSpec with Matchers {
  import ServerSentEventProtocol._

  "A FlowEvent" should {
    "be convertible to a ServerSentEvent, if it is a FlowRegistered" in {
      val serverSentEvent = flowEventToServerSentEvent(FlowFacade.FlowAdded(FlowFacade.FlowDescriptor("akka", "Akka")))
      val expectedData = """|{
                            |  "name": "akka",
                            |  "label": "Akka"
                            |}""".stripMargin
      serverSentEvent.data shouldBe expectedData
      serverSentEvent.eventType shouldBe Some("added")
    }

    "be convertible to a ServerSentEvent, if it is a FlowUnregistered" in {
      val serverSentEvent = flowEventToServerSentEvent(FlowFacade.FlowRemoved("akka"))
      serverSentEvent.data shouldBe "akka"
      serverSentEvent.eventType shouldBe Some("removed")
    }
  }

  "A MessageEvent" should {
    "be convertible to a ServerSentEvent, if it is a MessageAdded" in {
      val message = Flow.Message("Akka rocks!", LocalDateTime.now())
      val serverSentEvent = messageEventToServerSentEvent(Flow.MessageAdded("akka", message))
      val expectedData = s"""|{
                             |  "flowName": "akka",
                             |  "message": ${JsonProtocol.messageFormat.write(message)}
                             |}""".stripMargin.parseJson
      serverSentEvent.data.parseJson shouldBe expectedData
      serverSentEvent.eventType shouldBe Some("added")
    }
  }
}
