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
import akka.stream.ActorMaterializer
import akka.pattern.{ ask, pipe }
import akka.stream.Materializer
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.CirceSupport
import scala.concurrent.ExecutionContext

object HttpService {

  private[reactiveflows] case class AddFlowRequest(label: String)
  private[reactiveflows] case class AddMessageRequest(text: String)

  private[reactiveflows] case object Stop

  // $COVERAGE-OFF$
  final val Name = "http-service"
  // $COVERAGE-ON$

  def props(address: String, port: Int, flowFacade: ActorRef, flowFacadeTimeout: Timeout): Props =
    Props(new HttpService(address, port, flowFacade, flowFacadeTimeout))

  private[reactiveflows] def route(
    httpService: ActorRef, flowFacade: ActorRef, flowFacadeTimeout: Timeout
  )(implicit ec: ExecutionContext, mat: Materializer) = {
    import CirceSupport._
    import Directives._
    import io.circe.generic.auto._
    import io.circe.java8.time._

    // format: OFF
    def assets =
      getFromResourceDirectory("web") ~
      pathSingleSlash(get(redirect("index.html", StatusCodes.PermanentRedirect)))

    def stop = pathSingleSlash {
      delete {
        complete {
          httpService ! Stop
          "Stopping ..."
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
            case unknownFlow: FlowUnknown               => complete(StatusCodes.NotFound -> unknownFlow)
          }
        } ~
        post {
          entity(as[AddMessageRequest]) {
            case AddMessageRequest(text) =>
              onSuccess(flowFacade ? AddMessage(flowName, text)) {
                case messageAdded: Flow.MessageAdded => complete(StatusCodes.Created -> messageAdded)
                case unknownFlow: FlowUnknown        => complete(StatusCodes.NotFound -> unknownFlow)
              }
          }
        }
      } ~
      path(Segment) { flowName =>
        delete {
          onSuccess(flowFacade ? RemoveFlow(flowName)) {
            case flowRemoved: FlowRemoved => complete(StatusCodes.NoContent)
            case unknownFlow: FlowUnknown => complete(StatusCodes.NotFound -> unknownFlow)
          }
        }
      } ~
      get {
        complete((flowFacade ? GetFlows).mapTo[Iterable[FlowDescriptor]])
      } ~
      post {
        entity(as[AddFlowRequest]) { addFlowRequest =>
          onSuccess(flowFacade ? AddFlow(addFlowRequest.label)) {
            case flowAdded: FlowAdded     => complete(StatusCodes.Created -> flowAdded)
            case existingFlow: FlowExists => complete(StatusCodes.Conflict -> existingFlow)
          }
        }
      }
    }
    // format: ON

    assets ~ stop ~ flows
  }
}

class HttpService(address: String, port: Int, flowFacade: ActorRef, flowFacadeTimeout: Timeout)
    extends Actor with ActorLogging {
  import HttpService._
  import context.dispatcher

  private implicit val mat = ActorMaterializer()

  Http(context.system)
    .bindAndHandle(route(self, flowFacade, flowFacadeTimeout), address, port)
    .pipeTo(self)

  override def receive = binding

  private def binding: Receive = {
    case serverBinding @ Http.ServerBinding(address) =>
      log.info("Listening on {}", address)
      context.become(bound(serverBinding))

    case Status.Failure(cause) =>
      log.error(cause, s"Can't bind to $address:$port")
      context.stop(self)
  }

  private def bound(serverBinding: Http.ServerBinding): Receive = {
    case Stop =>
      serverBinding.unbind()
      context.stop(self)
  }
}
