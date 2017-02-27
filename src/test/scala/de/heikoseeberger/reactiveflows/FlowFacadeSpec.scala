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

import akka.actor.ActorRef
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator.{ Changed, Subscribe }
import akka.cluster.ddata.{ DistributedData, LWWMapKey }
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.testkit.TestActor.KeepRunning
import akka.testkit.TestProbe
import java.time.LocalDateTime
import org.scalatest.{ Matchers, WordSpec }

final class FlowFacadeSpec extends WordSpec with Matchers with AkkaSpec {
  import Flow.{ AddMessage => _, GetMessages => _, _ }
  import FlowFacade._

  private implicit val cluster = Cluster(system)

  "FlowFacade" should {
    "correctly handle GetFlows, AddFlow and RemoveFlow commands" in {
      val sender             = TestProbe()
      implicit val senderRef = sender.ref

      val mediator   = TestProbe()
      val replicator = TestProbe()
      val flowFacade = system.actorOf(FlowFacade(mediator.ref, replicator.ref, system.deadLetters))

      replicator.expectMsg(Subscribe(LWWMapKey[FlowDesc]("flows"), flowFacade))

      flowFacade ! GetFlows
      sender.expectMsg(Flows(Set.empty))

      flowFacade ! AddFlow("")
      sender.expectMsg(BadCommand("label empty"))

      flowFacade ! AddFlow("Akka")
      sender.expectMsg(FlowAdded(FlowDesc("akka", "Akka")))
      mediator.expectMsg(Publish(className[FlowEvent], FlowAdded(FlowDesc("akka", "Akka"))))

      flowFacade ! AddFlow("Akka")
      sender.expectMsg(FlowExists(FlowDesc("akka", "Akka")))

      flowFacade ! GetFlows
      sender.expectMsg(Flows(Set(FlowDesc("akka", "Akka"))))

      flowFacade ! RemoveFlow("")
      sender.expectMsg(BadCommand("name empty"))

      flowFacade ! RemoveFlow("akka")
      sender.expectMsg(FlowRemoved("akka"))
      mediator.expectMsg(Publish(className[FlowEvent], FlowRemoved("akka")))

      flowFacade ! GetFlows
      sender.expectMsg(Flows(Set.empty))

      flowFacade ! RemoveFlow("akka")
      sender.expectMsg(FlowUnknown("akka"))
    }

    "correctly handle GetMessages and AddMessage commands" in {
      val sender             = TestProbe()
      implicit val senderRef = sender.ref

      val time = LocalDateTime.now()

      val flowShardRegion = TestProbe()
      flowShardRegion.setAutoPilot(
        (sender: ActorRef, msg: Any) =>
          msg match {
            case ("akka", Flow.GetMessages(Long.MaxValue, Short.MaxValue)) =>
              sender ! Messages(Vector(Message(0, "Akka rocks!", time)))
              KeepRunning
            case ("akka", Flow.AddMessage(text)) =>
              sender ! Flow.MessageAdded("akka", Message(1, text, time))
              KeepRunning
        }
      )
      val flowFacade =
        system.actorOf(FlowFacade(system.deadLetters, system.deadLetters, flowShardRegion.ref))

      flowFacade ! GetMessages("", Long.MaxValue, Short.MaxValue)
      sender.expectMsg(BadCommand("name empty"))

      flowFacade ! GetMessages("akka", Long.MaxValue, Short.MaxValue)
      sender.expectMsg(FlowUnknown("akka"))

      flowFacade ! AddFlow("Akka")
      sender.expectMsg(FlowAdded(FlowDesc("akka", "Akka")))

      flowFacade ! GetMessages("akka", Long.MaxValue, Short.MaxValue)
      sender.expectMsg(Messages(Vector(Message(0, "Akka rocks!", time))))

      flowFacade ! AddMessage("", "Scala rocks!")
      sender.expectMsg(BadCommand("name empty"))

      flowFacade ! AddMessage("scala", "Scala rocks!")
      sender.expectMsg(FlowUnknown("scala"))

      flowFacade ! AddMessage("akka", "Scala rocks!")
      sender.expectMsg(Flow.MessageAdded("akka", Message(1, "Scala rocks!", time)))
    }

    "correctly update DistributedData" in {
      val replicator = DistributedData(system).replicator
      val subscriber = TestProbe()
      val flowFacade =
        system.actorOf(FlowFacade(system.deadLetters, replicator, system.deadLetters))
      replicator ! Subscribe(flows, subscriber.ref)

      flowFacade ! AddFlow("Akka")
      subscriber.expectMsgPF() {
        case c @ Changed(`flows`) if c.get(flows).entries.keySet == Set("akka") => ()
      }

      flowFacade ! RemoveFlow("akka")
      subscriber.expectMsgPF() { case c @ Changed(`flows`) if c.get(flows).entries.isEmpty => () }
    }
  }
}
