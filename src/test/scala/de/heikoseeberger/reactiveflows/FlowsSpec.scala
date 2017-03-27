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
import org.scalatest.{ Matchers, WordSpec }

final class FlowsSpec extends WordSpec with Matchers with AkkaSpec {
  import Flows._

  "Flows" should {
    "correctly handle AddFlow and RemoveFlow commands" in {
      val sender             = TestProbe()
      implicit val senderRef = sender.ref
      val flows              = system.actorOf(Flows())

      flows ! AddFlow("Akka")
      sender.expectMsg(FlowAdded(FlowDesc("akka", "Akka")))

      flows ! AddFlow("akka")
      sender.expectMsg(FlowExists(FlowDesc("akka", "Akka")))

      flows ! RemoveFlow("akka")
      sender.expectMsg(FlowRemoved("akka"))

      flows ! RemoveFlow("akka")
      sender.expectMsg(FlowUnknown("akka"))
    }
  }
}
