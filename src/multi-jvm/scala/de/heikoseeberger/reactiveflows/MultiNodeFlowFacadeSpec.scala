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

import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData
import akka.cluster.sharding.ClusterSharding
import akka.remote.testkit.{ MultiNodeConfig, MultiNodeSpec }
import akka.testkit.{ TestDuration, TestProbe }
import com.typesafe.config.ConfigFactory
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import scala.concurrent.duration.DurationInt

object FlowFacadeSpecConfig extends MultiNodeConfig {

  val Vector(node1, node2) = Vector(12551, 12552).map(node)

  private def node(port: Int) = {
    commonConfig(ConfigFactory.load())
    val node = role(port.toString)
    nodeConfig(node)(
        ConfigFactory
          .parseString(s"akka.remote.netty.tcp.port = $port")
          .withFallback(ConfigFactory.load())
    )
    node
  }
}

class FlowFacadeSpecMultiJvmNode1 extends MultiNodeFlowFacadeSpec
class FlowFacadeSpecMultiJvmNode2 extends MultiNodeFlowFacadeSpec

abstract class MultiNodeFlowFacadeSpec
    extends MultiNodeSpec(FlowFacadeSpecConfig)
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  import Flow.{ AddMessage => _, GetMessages => _, _ }
  import FlowFacade._
  import FlowFacadeSpecConfig._

  "FlowFacade" should {
    "work as expected in a cluster" in {
      within(10.seconds.dilated) {
        awaitAssert {
          Cluster(system).state.members.size shouldBe 2
        }
      }

      val flowShardRegion = Flow.startSharding(system, system.deadLetters, 20)

      enterBarrier("ready")

      val flowFacade = system.actorOf(
          FlowFacade(system.deadLetters,
                     DistributedData(system).replicator,
                     flowShardRegion)
      )
      runOn(node1) {
        val sender             = TestProbe()
        implicit val senderRef = sender.ref
        flowFacade ! AddFlow("Akka")
        sender.expectMsg(FlowAdded(FlowDesc("akka", "Akka")))
        flowFacade ! FlowFacade.AddMessage("akka", "Akka")
        sender.expectMsgPF() {
          case MessageAdded("akka", Flow.Message(0, "Akka", _)) => ()
        }
      }
      runOn(node2) {
        val sender             = TestProbe()
        implicit val senderRef = sender.ref
        within(10.seconds.dilated) {
          sender.awaitAssert {
            flowFacade ! GetFlows
            sender.expectMsg(Flows(Set(FlowDesc("akka", "Akka"))))
          }
        }
      }

      enterBarrier("message-added")

      runOn(node2) {
        val sender             = TestProbe()
        implicit val senderRef = sender.ref
        flowFacade ! GetMessages("akka", 99, 99)
        sender.expectMsgPF() {
          case Messages(Vector(Message(0, "Akka", _))) => ()
        }
      }
    }
  }

  override def initialParticipants = roles.size

  override protected def beforeAll() = {
    super.beforeAll()
    multiNodeSpecBeforeAll()
  }

  override protected def afterAll() = {
    multiNodeSpecAfterAll()
    super.afterAll()
  }
}
