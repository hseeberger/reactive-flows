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

import cats.data.Xor.Right
import io.circe.jawn
import java.time.LocalDateTime
import org.scalatest.{ Matchers, WordSpec }

class ServerSentEventProtocolSpec extends WordSpec with Matchers {
  import CirceCodec._
  import ServerSentEventProtocol._
  import io.circe.generic.auto._

  "A FlowEvent" should {
    "be convertible to a ServerSentEvent, if it is a FlowAdded" in {
      val flowDescriptor = FlowFacade.FlowDescriptor("akka", "Akka")
      val serverSentEvent = flowEventToServerSentEvent(FlowFacade.FlowAdded(flowDescriptor))
      jawn.decode[FlowFacade.FlowDescriptor](serverSentEvent.data) shouldBe Right(flowDescriptor)
      serverSentEvent.eventType shouldBe Some("added")
    }

    "be convertible to a ServerSentEvent, if it is a FlowRemoved" in {
      val serverSentEvent = flowEventToServerSentEvent(FlowFacade.FlowRemoved("akka"))
      serverSentEvent.data shouldBe "akka"
      serverSentEvent.eventType shouldBe Some("removed")
    }
  }

  "A MessageEvent" should {
    "be convertible to a ServerSentEvent, if it is a MessageAdded" in {
      val messageAdded = Flow.MessageAdded("akka", Flow.Message("Akka rocks!", LocalDateTime.now().withNano(0)))
      val serverSentEvent = messageEventToServerSentEvent(messageAdded)
      jawn.decode[Flow.MessageAdded](serverSentEvent.data) shouldBe Right(messageAdded)
      serverSentEvent.eventType shouldBe Some("added")
    }
  }
}
