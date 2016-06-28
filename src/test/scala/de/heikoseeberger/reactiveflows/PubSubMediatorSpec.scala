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

import akka.testkit.TestProbe

class PubSubMediatorSpec extends BaseAkkaSpec {
  import PubSubMediator._

  "PubSubMediator" should {
    "correctly handle Publish and Subscribe messages" in {
      val key      = "test-events"
      val mediator = system.actorOf(PubSubMediator())

      val subscriber1 = TestProbe()
      mediator ! Subscribe(key, subscriber1.ref)

      val subscriber2 = TestProbe()
      mediator ! Subscribe(s"other-$key", subscriber2.ref)

      mediator ! Publish(key, "msg")
      subscriber1.expectMsg("msg")
      subscriber2.expectNoMsg()
    }
  }
}
