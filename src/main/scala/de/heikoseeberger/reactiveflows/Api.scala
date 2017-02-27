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
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes.{
  BadRequest,
  Conflict,
  Created,
  NoContent,
  NotFound,
  PermanentRedirect
}
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.{ Directives, Route }
import akka.pattern.{ ask, pipe }
import akka.stream.{ ActorMaterializer, OverflowStrategy }
import akka.stream.scaladsl.Source
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.CirceSupport
import de.heikoseeberger.akkasse.{ EventStreamMarshalling, ServerSentEvent }
import java.net.InetSocketAddress
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

object Api {

  final case class AddMessageRequest(text: String)

  final val Name = "api"

  def apply(address: String,
            port: Int,
            flowFacade: ActorRef,
            flowFacadeTimeout: FiniteDuration,
            mediator: ActorRef,
            eventBufferSize: Int): Props =
    Props(new Api(address, port, flowFacade, flowFacadeTimeout, mediator, eventBufferSize))

  def route(flowFacade: ActorRef,
            flowFacadeTimeout: Timeout,
            mediator: ActorRef,
            eventBufferSize: Int)(implicit ec: ExecutionContext): Route = {
    import CirceSupport._
    import Directives._
    import EventStreamMarshalling._
    import Flow.{ AddMessage => _, GetMessages => _, _ }
    import FlowFacade._
    import io.circe.generic.auto._
    import io.circe.java8.time._
    import io.circe.syntax._

    def assets = {
      def redirectSingleSlash =
        pathSingleSlash {
          get {
            redirect("index.html", PermanentRedirect)
          }
        }
      getFromResourceDirectory("web") ~ redirectSingleSlash
    }

    def flows = {
      implicit val timeout = flowFacadeTimeout
      pathPrefix("flows") {
        pathEnd {
          get {
            complete((flowFacade ? GetFlows).mapTo[Flows].map(_.flows))
          } ~
          post {
            entity(as[AddFlow]) { addFlow =>
              onSuccess(flowFacade ? addFlow) {
                case fa: FlowAdded  => completeCreated(fa.desc.name, fa)
                case fe: FlowExists => complete(Conflict -> fe)
                case bc: BadCommand => complete(BadRequest -> bc)
              }
            }
          }
        } ~
        pathPrefix(Segment) { flowName =>
          pathEnd {
            delete {
              onSuccess(flowFacade ? RemoveFlow(flowName)) {
                case _: FlowRemoved  => complete(NoContent)
                case fu: FlowUnknown => complete(NotFound -> fu)
                // BadCommand not possible, because flowName can't be empty!
              }
            }
          } ~
          path("messages") {
            get {
              parameters('id.as[Long] ? Long.MaxValue, 'count.as[Short] ? 1.toShort) {
                (id, count) =>
                  onSuccess(flowFacade ? GetMessages(flowName, id, count)) {
                    case Messages(msgs)  => complete(msgs)
                    case fu: FlowUnknown => complete(NotFound -> fu)
                    // BadCommand not possible, because flowName can't be empty!
                  }
              }
            } ~
            post {
              entity(as[AddMessageRequest]) {
                case AddMessageRequest(text) =>
                  onSuccess(flowFacade ? AddMessage(flowName, text)) {
                    case ma: MessageAdded => completeCreated(ma.message.id.toString, ma)
                    case fu: FlowUnknown  => complete(NotFound -> fu)
                    case bc: BadCommand   => complete(BadRequest -> bc)
                  }
              }
            }
          }
        }
      }
    }

    def flowEvents = {
      def toServerSentEvent(event: FlowEvent) =
        event match {
          case FlowAdded(desc)   => ServerSentEvent(desc.asJson.noSpaces, "added")
          case FlowRemoved(name) => ServerSentEvent(name, "removed")
        }
      path("flow-events") {
        get {
          complete {
            events(toServerSentEvent)
          }
        }
      }
    }

    def messageEvents = {
      def toServerSentEvent(event: MessageEvent) =
        event match {
          case ma: MessageAdded => ServerSentEvent(ma.asJson.noSpaces, "added")
        }
      path("message-events") {
        get {
          complete {
            events(toServerSentEvent)
          }
        }
      }
    }

    def completeCreated[A: ToEntityMarshaller](id: String, a: A) =
      extractUri { uri =>
        val location = Location(uri.withPath(uri.path / id))
        complete((Created, Vector(location), a))
      }

    def events[A: ClassTag](toServerSentEvent: A => ServerSentEvent) =
      Source
        .actorRef[A](eventBufferSize, OverflowStrategy.dropHead)
        .map(toServerSentEvent)
        .mapMaterializedValue(source => mediator ! Subscribe(className[A], source))

    assets ~ flows ~ flowEvents ~ messageEvents
  }
}

final class Api(address: String,
                port: Int,
                flowFacade: ActorRef,
                flowFacadeTimeout: FiniteDuration,
                mediator: ActorRef,
                eventBufferSize: Int)
    extends Actor
    with ActorLogging {
  import Api._
  import context.dispatcher

  private implicit val mat = ActorMaterializer()

  Http(context.system)
    .bindAndHandle(route(flowFacade, flowFacadeTimeout, mediator, eventBufferSize), address, port)
    .pipeTo(self)

  override def receive = {
    case Http.ServerBinding(address) => handleServerBinding(address)
    case Status.Failure(cause)       => handleBindFailure(cause)
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
