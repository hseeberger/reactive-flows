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

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.http.Http
import akka.http.model.StatusCodes
import akka.http.server.{ Directives, Route }
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ ImplicitFlowMaterializer, Source }
import akka.util.Timeout
import de.heikoseeberger.akkasse.{ EventStreamMarshalling, ServerSentEvent }
import de.heikoseeberger.reactiveflows.util.SprayJsonMarshalling
import scala.concurrent.Future
import scala.concurrent.duration.{ DurationInt, FiniteDuration }
import spray.json.{ DefaultJsonProtocol, PrettyPrinter, jsonWriter }

object HttpService {

  private case object Shutdown

  def props(interface: String, port: Int, askTimeout: FiniteDuration): Props =
    Props(new HttpService(interface, port)(askTimeout))

  implicit def flowEventToSseMessage(event: Flow.Event): ServerSentEvent =
    event match {
      case messageAdded: Flow.MessageAdded =>
        val data = PrettyPrinter(jsonWriter[Flow.MessageAdded].write(messageAdded))
        ServerSentEvent(data, "added")
    }

  implicit def flowRepositoryEventToSseMessage(event: FlowRepository.Event): ServerSentEvent =
    event match {
      case FlowRepository.FlowAdded(flowData) =>
        ServerSentEvent(PrettyPrinter(jsonWriter[FlowRepository.FlowData].write(flowData)), "added")
      case FlowRepository.FlowRemoved(name) =>
        ServerSentEvent(name, "removed")
    }
}

class HttpService(interface: String, port: Int)(implicit askTimeout: Timeout)
    extends Actor
    with ActorLogging
    with Directives
    with ImplicitFlowMaterializer
    with EventStreamMarshalling
    with SprayJsonMarshalling
    with DefaultJsonProtocol {

  import HttpService._
  import context.dispatcher

  Http()(context.system)
    .bind(interface, port)
    .startHandlingWith(route)
  log.info(s"Listening on $interface:$port")
  log.info(s"To shutdown, send GET request to http://$interface:$port/shutdown")

  override def receive: Receive =
    Actor.emptyBehavior

  protected def createFlowEventPublisher(): ActorRef =
    context.actorOf(FlowEventPublisher.props)

  protected def createFlowRepositoryEventPublisher(): ActorRef =
    context.actorOf(FlowRepositoryEventPublisher.props)

  private def route: Route =
    assets ~ shutdown ~ messages ~ flows

  private def assets: Route =
    // format: OFF
    path("") {
      getFromResource("web/index.html")
    } ~
    getFromResourceDirectory("web") // format: ON

  private def shutdown: Route =
    path("shutdown") {
      get {
        complete {
          context.system.scheduler.scheduleOnce(500 millis, self, Shutdown)
          log.info("Shutting down now ...")
          "Shutting down now ..."
        }
      }
    }

  private def messages: Route =
    path("messages") {
      get {
        complete {
          Source(ActorPublisher[Flow.Event](createFlowEventPublisher()))
        }
      }
    }

  private def flows: Route =
    // format: OFF
    pathPrefix("flows") {
      path(Segment / "messages") { flowName =>
        get {
          complete {
            FlowRepository(context.system)
              .find(flowName)
              .flatMap { flowData =>
                if (flowData.isEmpty)
                  Future.successful(StatusCodes.NotFound -> Nil)
                else
                  Flows(context.system)
                    .getMessages(flowName)
                    .map(messages => StatusCodes.OK -> messages)
              }
          }
        } ~
        post {
          entity(as[MessageRequest]) { messageRequest =>
            complete {
              FlowRepository(context.system)
                .find(flowName)
                .flatMap { flowData =>
                  if (flowData.isEmpty)
                    Future.successful(StatusCodes.NotFound)
                  else
                    Flows(context.system)
                      .addMessage(flowName)(messageRequest.text)
                      .map(_ => StatusCodes.Created)
                }
            }
          }
        }
      } ~
      path(Segment) { flowName =>
        delete {
          complete {
            FlowRepository(context.system)
              .remove(flowName)
              .mapTo[FlowRepository.RemoveFlowReponse]
              .map {
                case _: FlowRepository.FlowRemoved => StatusCodes.NoContent
                case _: FlowRepository.UnknownFlow => StatusCodes.NotFound
              }
          }
        }
      } ~
      get {
        parameter("events") { _ =>
          complete {
              Source(ActorPublisher[FlowRepository.Event](createFlowRepositoryEventPublisher()))
          }
        } ~
        complete {
          FlowRepository(context.system).findAll
        }
      } ~
      post {
        entity(as[FlowRepository.AddFlowRequest]) { flowRequest =>
          complete {
            FlowRepository(context.system)
              .add(flowRequest)
              .mapTo[FlowRepository.AddFlowReponse]
              .map {
                case _: FlowRepository.FlowAdded             => StatusCodes.Created
                case _: FlowRepository.FlowAlreadyRegistered => StatusCodes.Conflict
              }
          }
        }
      }
    } // format: ON
}
