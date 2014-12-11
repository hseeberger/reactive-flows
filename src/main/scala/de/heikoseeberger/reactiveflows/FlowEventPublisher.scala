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

import akka.actor.{ ActorLogging, Props }
import akka.contrib.pattern.{ DistributedPubSubExtension, DistributedPubSubMediator }
import akka.stream.actor.{ ActorPublisher, ActorPublisherMessage }

object FlowEventPublisher {
  def props: Props =
    Props(new FlowEventPublisher)
}

class FlowEventPublisher
    extends ActorPublisher[Flow.Event]
    with ActorLogging {

  private val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(Flow.EventKey, self)
  log.debug("Subscribed to flow events")

  override def receive: Receive = {
    case event: Flow.Event if isActive && totalDemand > 0 => sourceEvent(event)
    case event: Flow.Event                                => log.warning("Can't source event [{}]", event)
    case ActorPublisherMessage.Cancel                     => context.stop(self)
  }

  private def sourceEvent(event: Flow.Event): Unit = {
    onNext(event)
    log.debug("Sourced event [{}]", event)
  }
}
