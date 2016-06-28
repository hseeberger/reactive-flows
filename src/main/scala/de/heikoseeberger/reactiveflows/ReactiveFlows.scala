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
  ActorContext,
  ActorLogging,
  ActorRef,
  ActorSystem,
  Props,
  SupervisorStrategy,
  Terminated
}
import scala.concurrent.Await
import scala.concurrent.duration.{ Duration, FiniteDuration }

object ReactiveFlows {

  type CreateMediator   = ActorContext => ActorRef
  type CreateFlowFacade = (ActorContext, ActorRef) => ActorRef
  type CreateApi = (ActorContext, String, Int, ActorRef, FiniteDuration,
                    ActorRef, Int) => ActorRef

  private val jvmArg = """-D(\S+)=(\S+)""".r

  def main(args: Array[String]): Unit = {
    for (jvmArg(name, value) <- args) System.setProperty(name, value)
    val system = ActorSystem("reactive-flows")
    system.actorOf(ReactiveFlows(), "root")
    Await.ready(system.whenTerminated, Duration.Inf)
  }

  def apply(createMediator: CreateMediator = createMediator,
            createFlowFacade: CreateFlowFacade = createFlowFacade,
            createApi: CreateApi = createApi): Props =
    Props(new ReactiveFlows(createMediator, createFlowFacade, createApi))

  private def createMediator(context: ActorContext) =
    context.actorOf(PubSubMediator(), PubSubMediator.Name)

  private def createFlowFacade(context: ActorContext, mediator: ActorRef) =
    context.actorOf(FlowFacade(mediator), FlowFacade.Name)

  private def createApi(context: ActorContext,
                        address: String,
                        port: Int,
                        flowFacade: ActorRef,
                        flowFacadeTimeout: FiniteDuration,
                        mediator: ActorRef,
                        eventBufferSize: Int) =
    context.actorOf(Api(address,
                        port,
                        flowFacade,
                        flowFacadeTimeout,
                        mediator,
                        eventBufferSize),
                    Api.Name)
}

import ReactiveFlows._

final class ReactiveFlows(createMediator: CreateMediator,
                          createFlowFacade: CreateFlowFacade,
                          createApi: CreateApi)
    extends Actor
    with ActorLogging {

  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  private val mediator = createMediator(context)

  private val flowFacade = createFlowFacade(context, mediator)

  private val api = {
    val config  = context.system.settings.config
    val address = config.getString("reactive-flows.api.address")
    val port    = config.getInt("reactive-flows.api.port")
    val timeout = config.getDuration("reactive-flows.api.flow-facade-timeout")
    val size    = config.getInt("reactive-flows.api.event-buffer-size")
    createApi(context, address, port, flowFacade, timeout, mediator, size)
  }

  context.watch(mediator)
  context.watch(flowFacade)
  context.watch(api)
  log.info("ReactiveFlows up and running")

  override def receive = {
    case Terminated(actor) => handleTerminated(actor)
  }

  private def handleTerminated(actor: ActorRef): Unit = {
    log.error("Terminating the system because {} terminated!", actor.path)
    context.system.terminate()
  }
}
