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

  case class Publish(key: String, message: Any)

  case class Subscribe(key: String, subscriber: ActorRef)

  val Name = "pub-sub-mediator"

  def props: Props = Props(new PubSubMediator)
}

class PubSubMediator extends Actor {

  import PubSubMediator._

  private var subscribers = Map.empty[String, Set[ActorRef]]

  override def receive = {
    case Publish(key, message)      => subscribers.getOrElse(key, Set.empty).foreach(_ ! message)
    case Subscribe(key, subscriber) => subscribe(key, subscriber)
    case Terminated(subscriber)     => subscribers = subscribers.map { case (k, vs) => k -> (vs - subscriber) }
  }

  private def subscribe(key: String, subscriber: ActorRef) = {
    subscribers += key -> (subscribers.getOrElse(key, Set.empty) + subscriber)
    context.watch(subscriber)
  }
}
