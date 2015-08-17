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

import akka.actor.{ ActorDSL, ActorIdentity, ActorRef, Identify, Props }
import akka.testkit.{ EventFilter, TestProbe }

class ReactiveFlowsSpec extends BaseAkkaSpec {
  import ActorDSL._

  "Creating a ReactiveFlows actor" should {
    "result in creating a FlowFacade child actor" in {
      val reactiveFlows = system.actorOf(ReactiveFlows.props)
      TestProbe().expectActor(reactiveFlows.path / FlowFacade.Name)
    }
  }

  "ReactiveFlows" should {
    "terminate the system upon termination of a child actor" in {
      val probe = TestProbe()
      actor(new ReactiveFlows {
        override protected def createFlowFacade() = actor(context)(new Act {
          context.stop(self)
        })
        override protected def onTerminated(actor: ActorRef) = probe.ref ! "terminated"
      })

      probe.expectMsg("terminated")
    }
  }
}
