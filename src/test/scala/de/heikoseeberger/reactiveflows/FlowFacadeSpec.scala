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

import akka.actor.{ ActorDSL, ActorIdentity, ActorRef, Identify }
import akka.cluster.Cluster
import akka.cluster.ddata.{ DistributedData, LWWMapKey, Replicator }
import akka.cluster.pubsub.DistributedPubSubMediator
import akka.testkit.{ TestActor, TestProbe }
import java.time.LocalDateTime

class FlowFacadeSpec extends BaseAkkaSpec {
  import ActorDSL._
  import FlowFacade._

  implicit val cluster = Cluster(system)

  "A FlowFacade actor" should {
    "correctly handle GetFlows, AddFlow and RemoveFlow commands" in {
      val sender = TestProbe()
      implicit val senderRef = sender.ref

      val mediator = TestProbe()
      val replicator = TestProbe()
      val flowFacade = system.actorOf(FlowFacade.props(mediator.ref, replicator.ref))

      replicator.expectMsg(Replicator.Subscribe(LWWMapKey[FlowDescriptor]("flows"), flowFacade))

      flowFacade ! GetFlows
      sender.expectMsg(Set.empty)

      flowFacade ! AddFlow("Akka")
      sender.expectMsg(FlowAdded(FlowDescriptor("akka", "Akka")))
      sender.expectActor(flowFacade.path / "akka")
      mediator.expectMsg(DistributedPubSubMediator.Publish(className[FlowEvent], FlowAdded(FlowDescriptor("akka", "Akka"))))

      flowFacade ! AddFlow("Akka")
      sender.expectMsg(FlowExists("Akka"))

      flowFacade ! GetFlows
      sender.expectMsg(Set(FlowDescriptor("akka", "Akka")))

      flowFacade ! RemoveFlow("akka")
      sender.expectMsg(FlowRemoved("akka"))
      sender.expectNoActor(flowFacade.path / "akka")
      mediator.expectMsg(DistributedPubSubMediator.Publish(className[FlowEvent], FlowRemoved("akka")))

      flowFacade ! RemoveFlow("akka")
      sender.expectMsg(FlowUnknown("akka"))

      flowFacade ! GetFlows
      sender.expectMsg(Set.empty)
    }

    "correctly handle GetMessages and AddMessage commands" in {
      val sender = TestProbe()
      implicit val senderRef = sender.ref

      val time = LocalDateTime.now()

      val flow = TestProbe()
      flow.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any) = {
          msg match {
            case Flow.GetMessages =>
              sender ! Vector(Flow.Message("Akka rocks!", time))
              TestActor.KeepRunning
            case Flow.AddMessage(text) =>
              sender ! Flow.MessageAdded("akka", Flow.Message(text, time))
              TestActor.KeepRunning
          }
        }
      })
      val mediator = TestProbe()
      val replicator = TestProbe()
      val flowFacade = actor(new FlowFacade(mediator.ref, replicator.ref) {
        override protected def createFlow(name: String) = actor(context, name)(new Act {
          become { case message => flow.ref.forward(message) }
        })
      })

      flowFacade ! GetMessages("akka")
      sender.expectMsg(FlowUnknown("akka"))

      flowFacade ! AddFlow("Akka")
      sender.expectMsg(FlowAdded(FlowDescriptor("akka", "Akka")))
      flowFacade ! GetMessages("akka")
      sender.expectMsg(Vector(Flow.Message("Akka rocks!", time)))

      flowFacade ! AddMessage("akka", "Akka really rocks!")
      sender.expectMsg(Flow.MessageAdded("akka", Flow.Message("Akka really rocks!", time)))
    }

    "correctly update DistributedData" in {
      val mediator = TestProbe()
      val replicator = DistributedData(system).replicator
      val subscriber = TestProbe()
      val flowFacade = system.actorOf(FlowFacade.props(mediator.ref, replicator))
      replicator ! Replicator.Subscribe(FlowFacade.flowReplicatorKey, subscriber.ref)

      flowFacade ! AddFlow("Akka")
      subscriber.expectMsgPF() {
        case changed @ Replicator.Changed(`flowReplicatorKey`) if changed.get(flowReplicatorKey).entries.keySet == Set("akka") => ()
      }

      flowFacade ! RemoveFlow("akka")
      subscriber.expectMsgPF() {
        case changed @ Replicator.Changed(`flowReplicatorKey`) if changed.get(flowReplicatorKey).entries.isEmpty => ()
      }
    }
  }
}
