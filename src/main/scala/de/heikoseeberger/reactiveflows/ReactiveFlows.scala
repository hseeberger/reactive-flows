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

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, SupervisorStrategy, Terminated }

object ReactiveFlows {

  final val Name = "reactive-flows"

  def props = Props(new ReactiveFlows)
}

class ReactiveFlows extends Actor with ActorLogging with SettingsActor {

  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  context.watch(createHttpService())

  override def receive = {
    case Terminated(actor) => onTerminated(actor)
  }

  protected def createHttpService(): ActorRef = context.actorOf(
    HttpService.props(settings.httpService.interface, settings.httpService.port),
    HttpService.Name
  )

  protected def onTerminated(actor: ActorRef): Unit = {
    log.error("Shutting down the system because {} terminated!", actor)
    context.system.shutdown()
  }
}
