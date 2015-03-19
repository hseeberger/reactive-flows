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

import akka.contrib.pattern.DistributedPubSubMediator
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ Sink, Source }
import akka.testkit.TestProbe
import de.heikoseeberger.akkasse.ServerSentEvent
import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class MessageEventPublisherSpec extends BaseAkkaSpec {

  implicit val mat = ActorFlowMaterializer()

  "A MessageEventPublisherSpec" should {

    "subscribe to message events and publish those" in {
      val mediator = TestProbe()
      val messageEventPublisher = system.actorOf(MessageEventPublisher.props(mediator.ref, 10))
      mediator.expectMsg(DistributedPubSubMediator.Subscribe(Flow.MessageEventKey, messageEventPublisher))

      val messageEvent = Source(ActorPublisher[ServerSentEvent](messageEventPublisher)).runWith(Sink.head)
      messageEventPublisher ! Flow.MessageAdded("akka", Flow.Message("Akka rocks!", LocalDateTime.now()))
      val ServerSentEvent(data, eventType) = Await.result(messageEvent, 1 second)
      data should (include("akka") and include("Akka rocks!"))
      eventType shouldBe Some("added")
    }
  }
}
