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

import akka.actor.{ Actor, ExtendedActorSystem, Extension, ExtensionKey }
import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS }

object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {

  object httpService {

    val interface: String =
      reactiveFlows.getString("http-service.interface")

    val port: Int =
      reactiveFlows.getInt("http-service.port")

    val askTimeout: FiniteDuration =
      FiniteDuration(reactiveFlows.getDuration("http-service.ask-timeout", MILLISECONDS), MILLISECONDS)
  }

  object flowRepository {

    val readTimeout: FiniteDuration =
      FiniteDuration(reactiveFlows.getDuration("flow-repository.read-timeout", MILLISECONDS), MILLISECONDS)

    val writeTimeout: FiniteDuration =
      FiniteDuration(reactiveFlows.getDuration("flow-repository.write-timeout", MILLISECONDS), MILLISECONDS)
  }

  object flowSharding {
    val shardCount: Int =
      reactiveFlows.getInt("flow-sharding.shard-count")
  }

  private val reactiveFlows = system.settings.config.getConfig("reactive-flows")
}

trait SettingsActor {
  this: Actor =>

  val settings: Settings =
    Settings(context.system)
}
