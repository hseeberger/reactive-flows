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
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.{ Directives, Route }
import akka.pattern.{ ask, pipe }
import akka.stream.{ ActorMaterializer, OverflowStrategy }
import akka.stream.scaladsl.Source
import akka.util.Timeout
import java.net.InetSocketAddress
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

object Api {

  final case class AddPostRequest(text: String)

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
    import Directives._
    import EventStreamMarshalling._
    import FailFastCirceSupport._
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
          import FlowFacade._
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
            import FlowFacade._
            delete {
              onSuccess(flowFacade ? RemoveFlow(flowName)) {
                case _: FlowRemoved  => complete(NoContent)
                case fu: FlowUnknown => complete(NotFound -> fu)
                // BadCommand not possible, because flowName can't be empty!
              }
            }
          } ~
          path("posts") {
            get {
              parameters('seqNo.as[Long] ? Long.MaxValue, 'count.as[Int] ? 1) { (id, count) =>
                onSuccess(flowFacade ? FlowFacade.GetPosts(flowName, id, count)) {
                  case Flow.Posts(posts)          => complete(posts)
                  case fu: FlowFacade.FlowUnknown => complete(NotFound -> fu)
                  // BadCommand not possible, because flowName can't be empty!
                }
              }
            } ~
            post {
              entity(as[AddPostRequest]) {
                case AddPostRequest(text) =>
                  onSuccess(flowFacade ? FlowFacade.AddPost(flowName, text)) {
                    case ma: Flow.PostAdded         => completeCreated(ma.post.seqNo.toString, ma)
                    case fu: FlowFacade.FlowUnknown => complete(NotFound -> fu)
                    case bc: BadCommand             => complete(BadRequest -> bc)
                  }
              }
            }
          }
        }
      }
    }

    def flowsEvents = {
      import FlowFacade._
      def toServerSentEvent(event: Event) =
        event match {
          case FlowAdded(desc)   => ServerSentEvent(desc.asJson.noSpaces, "added")
          case FlowRemoved(name) => ServerSentEvent(name, "removed")
        }
      path("flows-events") {
        get {
          complete {
            events(toServerSentEvent)
          }
        }
      }
    }

    def flowEvents = {
      import Flow._
      def toServerSentEvent(event: Event) =
        event match {
          case postAdded: PostAdded => ServerSentEvent(postAdded.asJson.noSpaces, "added")
        }
      path("flow-events") {
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

    assets ~ flows ~ flowsEvents ~ flowEvents
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
