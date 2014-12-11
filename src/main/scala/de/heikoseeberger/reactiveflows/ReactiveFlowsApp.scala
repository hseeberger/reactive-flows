/*
 * Copyright 2014 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.reactiveflows

import akka.actor.{ ActorRef, Actor, ActorLogging, ActorSystem, Props, SupervisorStrategy, Terminated }
import akka.event.LoggingAdapter
import de.heikoseeberger.reactiveflows.util.BaseApp

object ReactiveFlowsApp extends BaseApp[Unit] {

  override def run(system: ActorSystem, log: LoggingAdapter): Unit =
    system.actorOf(Reaper.props, "reaper")
}

object Reaper {
  def props: Props =
    Props(new Reaper)
}

class Reaper
    extends Actor
    with ActorLogging
    with SettingsActor {

  override val supervisorStrategy: SupervisorStrategy =
    SupervisorStrategy.stoppingStrategy

  context.watch(createHttpService())

  override def receive: Receive = {
    case Terminated(ref) =>
      log.warning("Shutting down, because {} has terminated!", ref.path)
      context.system.shutdown()
  }

  protected def createHttpService(): ActorRef = {
    import settings.httpService._
    context.actorOf(HttpService.props(interface, port), "http-service")
  }
}
