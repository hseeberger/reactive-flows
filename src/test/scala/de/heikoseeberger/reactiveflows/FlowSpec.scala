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

class FlowSpec extends BaseAkkaSpec {
  import Flow._

  "Flow" should {
    "correctly handle GetMessage and AddMessage commands" in {
      val sender             = TestProbe()
      implicit val senderRef = sender.ref

      val flow     = system.actorOf(Flow())
      val flowName = flow.path.name

      flow ! GetMessages(-1, 1)
      sender.expectMsg(BadCommand("id < 0"))

      flow ! GetMessages(0, 0)
      sender.expectMsg(BadCommand("count <= 0"))

      flow ! GetMessages(Long.MaxValue, 1)
      sender.expectMsg(Messages(Vector.empty))

      flow ! AddMessage("")
      sender.expectMsg(BadCommand("text empty"))

      flow ! AddMessage("Akka")
      val time0 = sender.expectMsgPF() {
        case MessageAdded(`flowName`, Message(0, "Akka", time)) => time
      }

      flow ! GetMessages(0, 1)
      sender.expectMsg(Messages(Vector(Message(0, "Akka", time0))))
      flow ! GetMessages(0, Short.MaxValue)
      sender.expectMsg(Messages(Vector(Message(0, "Akka", time0))))

      flow ! AddMessage("Scala")
      val time1 = sender.expectMsgPF() {
        case MessageAdded(`flowName`, Message(1, "Scala", time)) => time
      }
      flow ! AddMessage("Awesome")
      val time2 = sender.expectMsgPF() {
        case MessageAdded(`flowName`, Message(2, "Awesome", time)) => time
      }

      flow ! GetMessages(0, 1)
      sender.expectMsg(Messages(Vector(Message(0, "Akka", time0))))
      flow ! GetMessages(1, 1)
      sender.expectMsg(Messages(Vector(Message(1, "Scala", time1))))
      flow ! GetMessages(2, 1)
      sender.expectMsg(Messages(Vector(Message(2, "Awesome", time2))))
      flow ! GetMessages(1, Short.MaxValue)
      sender.expectMsg(
        Messages(Vector(Message(1, "Scala", time1), Message(0, "Akka", time0)))
      )
      flow ! GetMessages(Long.MaxValue, 3)
      sender.expectMsg(
        Messages(
          Vector(Message(2, "Awesome", time2),
                 Message(1, "Scala", time1),
                 Message(0, "Akka", time0))
        )
      )
    }
  }
}
