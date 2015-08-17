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

import akka.actor.{ Actor, ActorRef, Props, Terminated }

object PubSubMediator {

  case class Publish(topic: String, message: Any)

  case class Subscribe(topic: String, subscriber: ActorRef)

  // $COVERAGE-OFF$
  final val Name = "pub-sub-mediator"
  // $COVERAGE-ON$

  def props: Props = Props(new PubSubMediator)
}

class PubSubMediator extends Actor {
  import PubSubMediator._

  private var subscribers = Map.empty[String, Set[ActorRef]].withDefaultValue(Set.empty)

  override def receive = {
    case Publish(topic, message)      => subscribers(topic).foreach(_.forward(message))
    case Subscribe(topic, subscriber) => subscribe(topic, subscriber)
    // $COVERAGE-OFF$
    case Terminated(subscriber)       => subscribers = subscribers.map { case (t, ss) => t -> (ss - subscriber) }
    // $COVERAGE-ON$
  }

  private def subscribe(topic: String, subscriber: ActorRef) = {
    subscribers += topic -> (subscribers(topic) + subscriber)
    context.watch(subscriber)
  }
}
