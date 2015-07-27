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

import akka.actor.{ ActorIdentity, Identify, Props }
import akka.testkit.{ EventFilter, TestProbe }

class ReactiveFlowsSpec extends BaseAkkaSpec {

  "Creating a ReactiveFlows actor" should {
    """result in logging "Up and running" at INFO level""" in {
      EventFilter.info(occurrences = 1, message = "Up and running").intercept {
        system.actorOf(Props(new ReactiveFlows))
      }
    }

    "result in creating a FlowFacade child actor" in {
      val sender = TestProbe()
      implicit val senderRef = sender.ref

      val reactiveFlows = system.actorOf(ReactiveFlows.props)
      sender.awaitAssert {
        system.actorSelection(reactiveFlows.path / FlowFacade.Name) ! Identify(None)
        sender.expectMsgPF() { case ActorIdentity(_, Some(_)) => () }
      }
    }
  }
}
