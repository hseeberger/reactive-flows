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
import akka.testkit.TestProbe

class FlowSpec extends BaseAkkaSpec {

  import Flow._

  "A Flow" should {

    "correctly handle GetMessage and AddMessage commands" in {
      val mediator = TestProbe()
      val flow = system.actorOf(Flow.props(mediator.ref))
      val flowName = flow.path.name

      val sender = TestProbe()
      implicit val senderRef = sender.ref

      flow ! GetMessages
      sender.expectMsg(Nil)

      flow ! AddMessage("Akka rocks!")
      val dateTime = sender.expectMsgPF() { case MessageAdded(`flowName`, Message("Akka rocks!", dt)) => dt }
      mediator.expectMsg(DistributedPubSubMediator.Publish(
        Flow.MessageEventKey,
        MessageAdded(`flowName`, Message("Akka rocks!", dateTime))
      ))

      flow ! GetMessages
      sender.expectMsg(List(Message("Akka rocks!", dateTime)))
    }
  }
}
