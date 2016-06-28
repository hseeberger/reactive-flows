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

import akka.actor.{ Actor, ActorLogging, Props, Status }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.PermanentRedirect
import akka.http.scaladsl.server.Directives
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import java.net.InetSocketAddress

object Api {

  final val Name = "api"

  def apply(address: String, port: Int): Props =
    Props(new Api(address, port))

  def route = {
    import Directives._
    def redirectSingleSlash = pathSingleSlash {
      get {
        redirect("index.html", PermanentRedirect)
      }
    }
    getFromResourceDirectory("web") ~ redirectSingleSlash
  }
}

class Api(address: String, port: Int) extends Actor with ActorLogging {
  import Api._
  import context.dispatcher

  private implicit val mat = ActorMaterializer()

  Http(context.system).bindAndHandle(route, address, port).pipeTo(self)

  override def receive = {
    case Http.ServerBinding(a) => handleServerBinding(a)
    case Status.Failure(c)     => handleBindFailure(c)
  }

  private def handleServerBinding(address: InetSocketAddress) = {
    log.info("Listening on {}", address)
    context.become(Actor.emptyBehavior)
  }

  private def handleBindFailure(cause: Throwable) = {
    log.error(cause, s"Can't bind to $address:$port!")
    context.stop(self)
  }
}
