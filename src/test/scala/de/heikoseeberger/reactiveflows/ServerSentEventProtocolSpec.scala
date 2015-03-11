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
import org.scalatest.{ Matchers, WordSpec }

class ServerSentEventProtocolSpec extends WordSpec with Matchers {

  import ServerSentEventProtocol._

  "A FlowEvent" should {

    "be convertible to a ServerSentEvent, if it is a FlowRegistered" in {
      val serverSentEvent = FlowFacade.FlowAdded(FlowFacade.FlowInfo("akka", "Akka")): ServerSentEvent
      val expectedData = """|{
                            |  "name": "akka",
                            |  "label": "Akka"
                            |}""".stripMargin
      serverSentEvent.data shouldBe expectedData
      serverSentEvent.eventType shouldBe Some("added")
    }

    "be convertible to a ServerSentEvent, if it is a FlowUnregistered" in {
      val serverSentEvent = FlowFacade.FlowRemoved("akka"): ServerSentEvent
      serverSentEvent.data shouldBe "akka"
      serverSentEvent.eventType shouldBe Some("removed")
    }
  }
}
