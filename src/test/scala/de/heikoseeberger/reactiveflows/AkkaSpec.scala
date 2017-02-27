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

import akka.actor.{ ActorIdentity, ActorPath, ActorRef, ActorSystem, Identify }
import akka.testkit.{ TestDuration, TestProbe }
import org.scalatest.{ BeforeAndAfterAll, Suite }
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait AkkaSpec extends BeforeAndAfterAll { this: Suite =>

  implicit class TestProbeOps(probe: TestProbe) {

    def expectActor(path: String)(implicit system: ActorSystem): ActorRef =
      expectActor(ActorPath.fromString(path))

    def expectActor(path: ActorPath)(implicit system: ActorSystem): ActorRef = {
      var actor = probe.system.deadLetters
      probe.awaitAssert {
        probe.system.actorSelection(path).tell(Identify(path), probe.ref)
        probe
          .expectMsgPF(100.milliseconds.dilated, s"actor under path $path") {
            case ActorIdentity(`path`, Some(a)) => actor = a
          }
      }
      actor
    }

    def expectNoActor(path: String)(implicit system: ActorSystem): Unit =
      expectNoActor(ActorPath.fromString(path))

    def expectNoActor(path: ActorPath)(implicit system: ActorSystem): Unit =
      probe.awaitAssert {
        probe.system.actorSelection(path).tell(Identify(path), probe.ref)
        probe.expectMsg(100.milliseconds.dilated, ActorIdentity(path, None))
      }
  }

  protected implicit val system = ActorSystem()

  override protected def afterAll() = {
    Await.ready(system.terminate(), 42.seconds)
    super.afterAll()
  }
}
