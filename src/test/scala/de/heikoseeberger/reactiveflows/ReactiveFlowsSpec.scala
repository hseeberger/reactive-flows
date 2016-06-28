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

import akka.actor.{ Actor, ActorSystem, Props }
import akka.testkit.{ TestDuration, TestProbe }
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ReactiveFlowsSpec extends BaseAkkaSpec {

  "Creating a ReactiveFlows actor" should {
    "result in creating an Api child actor" in {
      val reactiveFlows = system.actorOf(ReactiveFlows())
      TestProbe().expectActor(reactiveFlows.path / Api.Name)
    }
  }

  "ReactiveFlows" should {
    "terminate the system when its Api child actor terminates" in {
      implicit val system = ActorSystem()
      system.actorOf(
          ReactiveFlows((context, _, _) => context.actorOf(terminatingActor))
      )
      Await.ready(system.whenTerminated, 3.seconds.dilated)
    }

    "terminate the system when its Api child actor fails" in {
      implicit val system = ActorSystem()
      system.actorOf(
          ReactiveFlows((context, _, _) => context.actorOf(faultyActor))
      )
      Await.ready(system.whenTerminated, 3.seconds)
    }
  }

  private def terminatingActor =
    Props(new Actor {
      context.stop(self)
      override def receive = Actor.emptyBehavior
    })

  private def faultyActor =
    Props(new Actor {
      self ! "I shall fail!"
      override def receive = {
        case _ => throw new Exception("Faulty by design!")
      }
    })
}
