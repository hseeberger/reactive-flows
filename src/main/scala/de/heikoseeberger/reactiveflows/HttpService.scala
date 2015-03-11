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
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.pattern.{ ask, pipe }
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{ ImplicitFlowMaterializer, Sink }
import akka.util.Timeout
import de.heikoseeberger.akkahttpjsonspray.SprayJsonMarshalling
import scala.concurrent.ExecutionContext

object HttpService {

  case class AddFlowRequest(label: String)

  case class AddMessageRequest(text: String)

  private[reactiveflows] case object Shutdown

  final val Name = "http-service"

  def props(interface: String, port: Int, flowFacade: ActorRef, flowFacadeTimeout: Timeout) =
    Props(new HttpService(interface, port, flowFacade, flowFacadeTimeout))

  private[reactiveflows] def route(
    httpService: ActorRef, flowFacade: ActorRef, flowFacadeTimeout: Timeout
  )(implicit ec: ExecutionContext, mat: FlowMaterializer) = {
    import Directives._
    import JsonProtocol._
    import SprayJsonMarshalling._

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

    def flows = pathPrefix("flows") {
      import FlowFacade._
      implicit val timeout = flowFacadeTimeout
      path(Segment / "messages") { flowName =>
        get {
          onSuccess(flowFacade ? GetMessages(flowName)) {
            case messages: Seq[Flow.Message] @unchecked => complete(messages)
            case unknownFlow: UnknownFlow               => complete(StatusCodes.NotFound -> unknownFlow)
          }
        } ~
        post {
          entity(as[AddMessageRequest]) { case AddMessageRequest(text) =>
            onSuccess(flowFacade ? AddMessage(flowName, text)) {
              case messageAdded: Flow.MessageAdded => complete(messageAdded)
              case unknownFlow: UnknownFlow        => complete(StatusCodes.NotFound -> unknownFlow)
            }
          }
        }
      } ~
      path(Segment) { flowName =>
        delete {
          onSuccess(flowFacade ? RemoveFlow(flowName)) {
            case flowRemoved: FlowRemoved => complete(StatusCodes.NoContent)
            case unknownFlow: UnknownFlow => complete(StatusCodes.NotFound -> unknownFlow)
          }
        }
      } ~
      get {
        complete((flowFacade ? GetFlows).mapTo[Iterable[FlowInfo]])
      } ~
      post {
        entity(as[AddFlowRequest]) { addFlowRequest =>
          onSuccess(flowFacade ? AddFlow(addFlowRequest.label)) {
            case flowAdded: FlowAdded   => complete(StatusCodes.Created -> flowAdded)
            case flowExists: FlowExists => complete(StatusCodes.Conflict -> flowExists)
          }
        }
      }
    }
    // format: ON

    assets ~ shutdown ~ flows
  }
}

class HttpService(interface: String, port: Int, flowFacade: ActorRef, flowFacadeTimeout: Timeout)
    extends Actor with ActorLogging with ImplicitFlowMaterializer {

  import HttpService._
  import context.dispatcher

  serveHttp()

  override def receive = {
    case Http.ServerBinding(address) => log.info("Listening on {}", address)
    case Status.Failure(_)           => context.stop(self)
    case Shutdown                    => context.stop(self)
  }

  private def serveHttp() = Http(context.system)
    .bindAndHandle(route(self, flowFacade, flowFacadeTimeout), interface, port)
    .pipeTo(self)
}
