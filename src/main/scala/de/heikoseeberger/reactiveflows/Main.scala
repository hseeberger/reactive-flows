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

import akka.actor.{
  Actor,
  ActorLogging,
  ActorRef,
  ActorSystem,
  Props,
  SupervisorStrategy,
  Terminated
}
import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData
import akka.cluster.pubsub.DistributedPubSub
import akka.stream.ActorMaterializer

object Main {

  final class Root extends Actor with ActorLogging {

    override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

    private implicit val mat = ActorMaterializer()

    private val mediator = DistributedPubSub(context.system).mediator

    private val flowShardRegion = {
      val config             = context.system.settings.config
      val shardCount         = config.getInt("reactive-flows.flow.shard-count")
      val passivationTimeout = config.getDuration("reactive-flows.flow.passivation-timeout")
      Flow.startSharding(context.system, mediator, shardCount, passivationTimeout)
    }

    private val flowFacade = {
      val replicator = DistributedData(context.system).replicator
      context.actorOf(FlowFacade(mediator, replicator, flowShardRegion), FlowFacade.Name)
    }

    private val api = {
      val config     = context.system.settings.config
      val address    = config.getString("reactive-flows.api.address")
      val port       = config.getInt("reactive-flows.api.port")
      val timeout    = config.getDuration("reactive-flows.api.flow-facade-timeout")
      val bufferSize = config.getInt("reactive-flows.api.event-buffer-size")
      val heartbeat  = config.getDuration("reactive-flows.api.event-heartbeat")
      context.actorOf(Api(address, port, flowFacade, timeout, mediator, bufferSize, heartbeat),
                      Api.Name)
    }

    context.watch(flowFacade)
    context.watch(api)
    log.info("{} up and running", context.system.name)

    override def receive = {
      case Terminated(actor) =>
        log.error("Terminating the system because {} terminated!", actor.path)
        context.system.terminate()
    }
  }

  // Needed to terminate the actor system on initialization errors of root, e.g. missing configuration settings!
  final class Terminator(root: ActorRef) extends Actor with ActorLogging {

    context.watch(root)

    override def receive = {
      case Terminated(`root`) =>
        log.error("Terminating the system because root terminated!")
        context.system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("reactive-flows")
    Cluster(system).registerOnMemberUp {
      val root = system.actorOf(Props(new Root), "root")
      system.actorOf(Props(new Terminator(root)), "terminator")
    }
  }
}
