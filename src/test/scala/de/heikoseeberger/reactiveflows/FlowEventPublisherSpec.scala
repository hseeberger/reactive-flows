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
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class FlowEventPublisherSpec extends BaseAkkaSpec {

  implicit val mat = ActorFlowMaterializer()

  "A FlowEventPublisherSpec" should {

    "subscribe to flow events and publish those" in {
      val mediator = TestProbe()
      val flowEventPublisher = system.actorOf(FlowEventPublisher.props(mediator.ref, 10))
      mediator.expectMsg(DistributedPubSubMediator.Subscribe(FlowFacade.FlowEventKey, flowEventPublisher))

      val flowEvent = Source(ActorPublisher[ServerSentEvent](flowEventPublisher)).runWith(Sink.head)
      flowEventPublisher ! FlowFacade.FlowAdded(FlowFacade.FlowInfo("akka", "Akka"))
      val ServerSentEvent(data, eventType) = Await.result(flowEvent, 1 second)
      data should (include("akka") and include("Akka"))
      eventType shouldBe Some("added")
    }
  }
}
