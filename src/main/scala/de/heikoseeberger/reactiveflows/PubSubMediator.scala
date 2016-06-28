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

  type Topic = String

  final case class Publish(topic: Topic, message: Any)
  final case class Subscribe(topic: Topic, subscriber: ActorRef)

  final val Name = "pub-sub-mediator"

  def apply(): Props =
    Props(new PubSubMediator)
}

final class PubSubMediator extends Actor {
  import PubSubMediator._

  private var subscribers =
    Map.empty[Topic, Set[ActorRef]].withDefaultValue(Set.empty)

  override def receive = {
    case Publish(t, m)   => subscribers(t).foreach(_.forward(m))
    case Subscribe(t, s) => handleSubscribe(t, s)
    case Terminated(s)   => handleTerminated(s)
  }

  private def handleSubscribe(topic: String, subscriber: ActorRef) = {
    subscribers += topic -> (subscribers(topic) + subscriber)
    context.watch(subscriber)
  }

  private def handleTerminated(subscriber: ActorRef) =
    subscribers = subscribers.map { case (t, ss) => t -> (ss - subscriber) }
}
