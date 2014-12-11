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

import akka.actor.{ ActorRef, ExtendedActorSystem, Extension, ExtensionKey, Props }
import akka.util.Timeout
import de.heikoseeberger.reactiveflows.util.Sharding
import scala.concurrent.{ ExecutionContext, Future }

object Flows extends ExtensionKey[Flows]

class Flows(protected val system: ExtendedActorSystem)
    extends Extension
    with Sharding {

  def getMessages(name: String)(implicit timeout: Timeout, sender: ActorRef): Future[Seq[Message]] =
    askEntry(name, Flow.GetMessages).mapTo[Seq[Message]]

  def addMessage(name: String)(text: String)(implicit timeout: Timeout, sender: ActorRef): Future[Flow.MessageAdded] =
    askEntry(name, Flow.AddMessage(text)).mapTo[Flow.MessageAdded]

  override protected def props: Props =
    Flow.props

  override protected def shardCount: Int =
    Settings(system).flowSharding.shardCount

  override protected def typeName: String =
    "flow"
}
