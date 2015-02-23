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

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Status }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.pattern.pipe
import akka.stream.BindFailedException
import akka.stream.scaladsl.{ ImplicitFlowMaterializer, Sink }
import java.net.InetSocketAddress
import scala.concurrent.ExecutionContext

object HttpService {

  private[reactiveflows] case object Shutdown

  final val Name = "http-service"

  def props(interface: String, port: Int) = Props(new HttpService(interface, port))

  private[reactiveflows] def route(httpService: ActorRef)(implicit ec: ExecutionContext) = {
    import Directives._

    // format: OFF
    def assets = getFromResourceDirectory("web") ~ path("")(getFromResource("web/index.html"))

    def shutdown = path("") {
      delete {
        complete {
          httpService ! Shutdown
          "Shutting down now ..."
        }
      }
    }
    // format: ON

    assets ~ shutdown
  }
}

class HttpService(interface: String, port: Int) extends Actor with ActorLogging with ImplicitFlowMaterializer {

  import HttpService._
  import context.dispatcher

  serveHttp()

  override def receive = {
    case Http.ServerBinding(address) => log.info("Listening on {}", address)
    case Status.Failure(_)           => context.stop(self)
    case Shutdown                    => context.stop(self)
  }

  private def serveHttp() = Http(context.system)
    .bindAndHandle(route(self), interface, port)
    .pipeTo(self)
}
