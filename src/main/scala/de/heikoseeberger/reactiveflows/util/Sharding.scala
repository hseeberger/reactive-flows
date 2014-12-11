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
package util

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.contrib.pattern.{ ClusterSharding, ShardRegion }
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Future

trait Sharding {

  def start(): Unit = {
    Logging(system, getClass).info("Starting shard region for type [{}]", typeName)
    ClusterSharding(system).start(typeName, Some(props), idExtractor, shardResolver(shardCount))
  }

  def tellEntry(name: String, message: Any)(implicit sender: ActorRef): Unit =
    shardRegion ! (name, message)

  def askEntry(name: String, message: Any)(implicit timeout: Timeout, sender: ActorRef): Future[Any] =
    shardRegion ? (name, message)

  protected def props: Props

  protected def shardCount: Int

  protected def typeName: String

  protected def system: ActorSystem

  private def idExtractor: ShardRegion.IdExtractor = {
    case (name: String, payload) => (name, payload)
  }

  private def shardResolver(shardCount: Int): ShardRegion.ShardResolver = {
    case (name: String, _) => (name.hashCode % shardCount).toString
  }

  private def shardRegion = ClusterSharding(system).shardRegion(typeName)
}
