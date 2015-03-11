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

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import akka.testkit.{ TestActor, TestProbe }
import java.time.LocalDateTime

class FlowFacadeSpec extends BaseAkkaSpec {

  import FlowFacade._

  val now = LocalDateTime.now()

  "FlowFacade" should {

    "correctly handle GetFlows, AddFlow and RemoveFlow commands" in {
      val flowFacade = system.actorOf(FlowFacade.props)

      val sender = TestProbe()
      implicit val senderRef = sender.ref

      flowFacade ! GetFlows
      sender.expectMsg(Set.empty)

      flowFacade ! AddFlow("Akka")
      sender.expectMsg(FlowAdded(FlowInfo("akka", "Akka")))

      flowFacade ! GetFlows
      sender.expectMsg(Set(FlowInfo("akka", "Akka")))

      flowFacade ! RemoveFlow("akka")
      sender.expectMsg(FlowRemoved("akka"))

      flowFacade ! GetFlows
      sender.expectMsg(Set.empty)
    }

    "correctly handle GetMessages and AddMessage commands" in {
      val flow = TestProbe()
      flow.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any) = {
          msg match {
            case Flow.GetMessages =>
              sender ! List(Flow.Message("Akka rocks!", now))
              TestActor.KeepRunning
            case Flow.AddMessage(text) =>
              sender ! Flow.MessageAdded("akka", Flow.Message(text, now))
              TestActor.KeepRunning
          }
        }
      })
      val flowFacade = actor(new FlowFacade {
        override protected def createFlow(name: String) = flow.ref
        override protected def forwardToFlow(name: String)(message: Any) = flow.ref.forward(message)
      })

      val sender = TestProbe()
      implicit val senderRef = sender.ref

      flowFacade ! GetMessages("akka")
      sender.expectMsg(UnknownFlow("akka"))

      flowFacade ! AddFlow("Akka")
      sender.expectMsg(FlowAdded(FlowInfo("akka", "Akka")))
      flowFacade ! GetMessages("akka")
      sender.expectMsg(List(Flow.Message("Akka rocks!", now)))

      flowFacade ! AddMessage("akka", "Akka really rocks!")
      sender.expectMsg(Flow.MessageAdded("akka", Flow.Message("Akka really rocks!", now)))
    }
  }
}
