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

import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.testkit.TestProbe
import scala.concurrent.duration.DurationInt
import org.scalatest.{ Matchers, WordSpec }

final class FlowSpec extends WordSpec with Matchers with AkkaSpec {
  import Flow._

  "Flow" should {
    "correctly handle GetPost and AddPost commands" in {
      val sender             = TestProbe()
      implicit val senderRef = sender.ref

      val mediator = TestProbe()
      val flow     = system.actorOf(Flow(mediator.ref, 1.hour))
      val flowName = flow.path.name

      flow ! GetPosts(-1, 1)
      sender.expectMsg(BadCommand("seqNo < 0"))

      flow ! GetPosts(0, 0)
      sender.expectMsg(BadCommand("count <= 0"))

      flow ! GetPosts(Long.MaxValue, 1)
      sender.expectMsg(Posts(Vector.empty))

      flow ! AddPost("")
      sender.expectMsg(BadCommand("text empty"))

      flow ! AddPost("Akka")
      val time0 =
        sender.expectMsgPF(hint = """expected `PostAdded(`flowName`, Post(2, "Akka", _))`""") {
          case PostAdded(`flowName`, Post(0, "Akka", time)) => time
        }
      mediator.expectMsg(Publish(className[Event], PostAdded(`flowName`, Post(0, "Akka", time0))))

      flow ! GetPosts(0, 1)
      sender.expectMsg(Posts(Vector(Post(0, "Akka", time0))))
      flow ! GetPosts(0, Int.MaxValue)
      sender.expectMsg(Posts(Vector(Post(0, "Akka", time0))))

      flow ! AddPost("Scala")
      val time1 =
        sender.expectMsgPF(hint = """expected `PostAdded(`flowName`, Post(2, "Scala", _))`""") {
          case PostAdded(`flowName`, Post(1, "Scala", time)) => time
        }
      flow ! AddPost("Awesome")
      val time2 =
        sender.expectMsgPF(hint = """expected `PostAdded(`flowName`, Post(2, "Awesome", _))`""") {
          case PostAdded(`flowName`, Post(2, "Awesome", time)) => time
        }

      flow ! GetPosts(0, 1)
      sender.expectMsg(Posts(Vector(Post(0, "Akka", time0))))
      flow ! GetPosts(1, 1)
      sender.expectMsg(Posts(Vector(Post(1, "Scala", time1))))
      flow ! GetPosts(2, 1)
      sender.expectMsg(Posts(Vector(Post(2, "Awesome", time2))))
      flow ! GetPosts(Long.MaxValue, 1)
      sender.expectMsg(Posts(Vector(Post(2, "Awesome", time2))))
      flow ! GetPosts(Long.MaxValue, 2)
      sender.expectMsg(Posts(Vector(Post(2, "Awesome", time2), Post(1, "Scala", time1))))
      flow ! GetPosts(Long.MaxValue, Int.MaxValue)
      sender.expectMsg(
        Posts(Vector(Post(2, "Awesome", time2), Post(1, "Scala", time1), Post(0, "Akka", time0)))
      )
    }
  }
}
