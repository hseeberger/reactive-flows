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

import akka.NotUsed
import akka.actor.{ ActorRef, Status, Terminated }
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes.{
  BadRequest,
  Conflict,
  Created,
  NoContent,
  NotFound,
  OK,
  PermanentRedirect
}
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.{ StatusCodes, Uri }
import akka.http.scaladsl.testkit.RouteTest
import akka.http.scaladsl.testkit.TestFrameworkInterface.Scalatest
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling
import akka.stream.scaladsl.{ Sink, Source }
import akka.testkit.TestActor.{ AutoPilot, NoAutoPilot }
import akka.testkit.{ TestDuration, TestProbe }
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.parser.decode
import java.time.Instant.now
import org.scalatest.{ AsyncWordSpec, Matchers, Succeeded }
import scala.concurrent.duration.DurationInt

final class ApiSpec extends AsyncWordSpec with Matchers with RouteTest with Scalatest {
  import Api._
  import EventStreamUnmarshalling._

  private val timeout = 250.milliseconds.dilated

  "Api" should {
    "stop itself when the HTTP binding fails" in {
      val probe = TestProbe()
      val apiProps = Api("127.0.0.1",
                         18000,
                         system.deadLetters,
                         100.milliseconds.dilated,
                         system.deadLetters,
                         99)
      def createAndWatch() = probe.watch(system.actorOf(apiProps))
      val api1             = createAndWatch()
      val api2             = createAndWatch()
      probe.expectMsgPF(hint = """expected `Terminated(api1 || api2)`""") {
        case Terminated(actor) if actor == api1 || actor == api2 => Succeeded
      }
    }
  }

  "Api.route" should {
    "respond with PermanentRedirect to index.html upon a 'GET /'" in {
      Get() ~> route(system.deadLetters, timeout, system.deadLetters, 99) ~> check {
        status shouldBe PermanentRedirect
        header[Location] shouldBe Some(Location(Uri("index.html")))
      }
    }

    "respond with OK upon a 'GET /test.html'" in {
      Get("/test.html") ~> route(system.deadLetters, timeout, system.deadLetters, 99) ~> check {
        status shouldBe OK
        responseAs[String].trim shouldBe "test"
      }
    }

    // Attention: Don't move these up, else the above test will fail due to content negotiation!
    import FailFastCirceSupport._
    import io.circe.generic.auto._
    import io.circe.java8.time._

    "ask FlowFacade GetFlows and respond with OK upon a 'GET /flows'" in {
      import FlowFacade._
      val flowFacade = TestProbe()
      val flows      = Set(FlowDesc("akka", "Akka"), FlowDesc("angularjs", "AngularJS"))
      flowFacade.setAutoPilot(
        (sender: ActorRef, msg: Any) =>
          msg match {
            case GetFlows =>
              sender ! Flows(flows)
              NoAutoPilot
        }
      )
      Get("/flows") ~> route(flowFacade.ref, timeout, system.deadLetters, 99) ~> check {
        status shouldBe OK
        responseAs[Set[FlowDesc]] shouldBe flows
      }
    }

    "ask FlowFacade AddFlow and respond with Created upon a 'POST /flows'" in {
      import FlowFacade._
      val flowFacade = TestProbe()
      val flowAdded  = FlowAdded(FlowDesc("akka", "Akka"))
      flowFacade.setAutoPilot(
        (sender: ActorRef, msg: Any) =>
          msg match {
            case AddFlow("Akka") =>
              sender ! flowAdded
              NoAutoPilot
        }
      )
      Post("/flows", AddFlow("Akka")) ~> route(flowFacade.ref, timeout, system.deadLetters, 99) ~> check {
        status shouldBe Created
        responseAs[FlowAdded] shouldBe flowAdded
      }
    }

    "ask FlowFacade AddFlow and respond with Conflict upon a 'POST /flows' with an existing flow" in {
      import FlowFacade._
      val flowFacade = TestProbe()
      val flowExists = FlowExists(FlowDesc("akka", "Akka"))
      flowFacade.setAutoPilot(
        (sender: ActorRef, msg: Any) =>
          msg match {
            case AddFlow("Akka") =>
              sender ! flowExists
              NoAutoPilot
        }
      )
      Post("/flows", AddFlow("Akka")) ~> route(flowFacade.ref, timeout, system.deadLetters, 99) ~> check {
        status shouldBe Conflict
        responseAs[FlowExists] shouldBe flowExists
      }
    }

    "ask FlowFacade AddFlow and respond with BadRequest upon a 'POST /flows' with an empty label" in {
      import FlowFacade._
      val flowFacade = TestProbe()
      val emptyLabel = "empty label"
      flowFacade.setAutoPilot(
        (sender: ActorRef, msg: Any) =>
          msg match {
            case AddFlow("") =>
              sender ! InvalidCommand(emptyLabel)
              NoAutoPilot
        }
      )
      Post("/flows", AddFlow("")) ~> route(flowFacade.ref, timeout, system.deadLetters, 99) ~> check {
        status shouldBe BadRequest
        responseAs[InvalidCommand] shouldBe InvalidCommand(emptyLabel)
      }
    }

    "ask FlowFacade RemoveFlow and respond with NoContent upon a 'DELETE /flows/name'" in {
      import FlowFacade._
      val flowFacade  = TestProbe()
      val flowRemoved = FlowRemoved("akka")
      flowFacade.setAutoPilot(
        (sender: ActorRef, msg: Any) =>
          msg match {
            case RemoveFlow("akka") =>
              sender ! flowRemoved
              NoAutoPilot
        }
      )
      Delete("/flows/akka") ~> route(flowFacade.ref, timeout, system.deadLetters, 99) ~> check {
        status shouldBe NoContent
      }
    }

    "ask FlowFacade RemoveFlow and respond with NotFound upon a 'DELETE /flows/name' with an unknown name" in {
      import FlowFacade._
      val flowFacade  = TestProbe()
      val flowUnknown = FlowUnknown("unknown")
      flowFacade.setAutoPilot(
        (sender: ActorRef, msg: Any) =>
          msg match {
            case RemoveFlow("unknown") =>
              sender ! flowUnknown
              NoAutoPilot
        }
      )
      Delete("/flows/unknown") ~> route(flowFacade.ref, timeout, system.deadLetters, 99) ~> check {
        status shouldBe NotFound
        responseAs[FlowUnknown] shouldBe flowUnknown
      }
    }

    "ask FlowFacade GetPosts and respond with OK upon a 'GET /flows/name/posts'" in {
      import Flow._
      val flowFacade = TestProbe()
      val posts      = Vector(Flow.Post(1, "m1", now()), Flow.Post(0, "m0", now()))
      flowFacade.setAutoPilot(
        (sender: ActorRef, msg: Any) =>
          msg match {
            case FlowFacade.GetPosts("akka", Long.MaxValue, 2) =>
              sender ! Posts(posts)
              NoAutoPilot
        }
      )
      Get("/flows/akka/posts?count=2") ~> route(flowFacade.ref, timeout, system.deadLetters, 99) ~> check {
        status shouldBe OK
        responseAs[Seq[Flow.Post]] shouldBe posts
      }
    }

    "ask FlowFacade GetPosts and respond with NotFound upon a 'GET /flows/name/posts' with an unknown name" in {
      import FlowFacade._
      val flowFacade  = TestProbe()
      val flowUnknown = FlowUnknown("unknown")
      flowFacade.setAutoPilot(
        (sender: ActorRef, msg: Any) =>
          msg match {
            case GetPosts("unknown", 1, 1) =>
              sender ! flowUnknown
              NoAutoPilot
        }
      )
      Get("/flows/unknown/posts?seqNo=1") ~> route(flowFacade.ref, timeout, system.deadLetters, 99) ~> check {
        status shouldBe NotFound
        responseAs[FlowUnknown] shouldBe flowUnknown
      }
    }

    "ask FlowFacade AddPosts and respond with Created upon a 'POST /flows/name/posts'" in {
      import Flow._
      val flowFacade = TestProbe()
      val postAdded  = PostAdded("akka", Flow.Post(0, "text", now()))
      flowFacade.setAutoPilot(
        (sender: ActorRef, msg: Any) =>
          msg match {
            case FlowFacade.AddPost("akka", "text") =>
              sender ! postAdded
              NoAutoPilot
        }
      )
      val request = RequestBuilding.Post("/flows/akka/posts", AddPostRequest("text"))
      request ~> route(flowFacade.ref, timeout, system.deadLetters, 99) ~> check {
        status shouldBe Created
        responseAs[PostAdded] shouldBe postAdded
      }
    }

    "ask FlowFacade AddPosts and respond with NotFound upon a 'POST /flows/name/posts' with an unknown name" in {
      import FlowFacade._
      val flowFacade  = TestProbe()
      val flowUnknown = FlowUnknown("unknown")
      flowFacade.setAutoPilot(
        (sender: ActorRef, msg: Any) =>
          msg match {
            case AddPost("unknown", "text") =>
              sender ! flowUnknown
              NoAutoPilot
        }
      )
      val request = Post("/flows/unknown/posts", AddPostRequest("text"))
      request ~> route(flowFacade.ref, timeout, system.deadLetters, 99) ~> check {
        status shouldBe NotFound
        responseAs[FlowUnknown] shouldBe flowUnknown
      }
    }

    "ask FlowFacade AddPosts and respond with BadRequest upon a 'POST /flows/name/posts' with an empty text" in {
      import FlowFacade._
      val flowFacade = TestProbe()
      val emptyText  = "empty text"
      flowFacade.setAutoPilot(
        (sender: ActorRef, msg: Any) =>
          msg match {
            case AddPost("akka", "") =>
              sender ! InvalidCommand(emptyText)
              NoAutoPilot
        }
      )
      val request = Post("/flows/akka/posts", AddPostRequest(""))
      request ~> route(flowFacade.ref, timeout, system.deadLetters, 99) ~> check {
        status shouldBe BadRequest
        responseAs[InvalidCommand] shouldBe InvalidCommand(emptyText)
      }
    }

    "respond with OK upon a GET for '/flows-events'" in {
      import FlowFacade._
      val mediator      = TestProbe()
      val akkaFlow      = FlowDesc("akka", "Akka")
      val angularJsFlow = FlowDesc("angularjs", "AngularJS")
      mediator.setAutoPilot(new AutoPilot {
        private val flowEventTopic = className[Event]
        def run(sender: ActorRef, msg: Any) =
          msg match {
            case Subscribe(`flowEventTopic`, _, source) =>
              source ! FlowAdded(akkaFlow)
              source ! FlowAdded(angularJsFlow)
              source ! Status.Success(NotUsed) // Completes the Source.actorRef!
              NoAutoPilot
          }
      })
      val request = Get("/flows-events")
      request ~> route(system.deadLetters, timeout, mediator.ref, 99) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Source[ServerSentEvent, NotUsed]]
          .collect { case ServerSentEvent(d, Some(tpe), _, _) => (decode[FlowDesc](d), tpe) }
          .collect { case (Right(postAdded), tpe) => (postAdded, tpe) }
          .runWith(Sink.seq)
          .map(_ shouldBe Vector((akkaFlow, "added"), (angularJsFlow, "added")))
      }
    }

    "respond with OK upon a GET for '/flow-events'" in {
      import Flow._
      val mediator           = TestProbe()
      val akkaPostAdded      = PostAdded("akka", Flow.Post(0, "Akka", now()))
      val angularJsPostAdded = PostAdded("angularjs", Flow.Post(1, "Scala", now()))
      mediator.setAutoPilot(new AutoPilot {
        private val postEventTopic = className[Event]
        def run(sender: ActorRef, msg: Any) =
          msg match {
            case Subscribe(`postEventTopic`, _, source) =>
              source ! akkaPostAdded
              source ! angularJsPostAdded
              source ! Status.Success(None)
              NoAutoPilot
          }
      })
      val request = Get("/flow-events")
      request ~> route(system.deadLetters, timeout, mediator.ref, 99) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Source[ServerSentEvent, NotUsed]]
          .collect {
            case ServerSentEvent(data, Some(eventType), _, _) =>
              (decode[PostAdded](data), eventType)
          }
          .collect {
            case (Right(postAdded), eventType) => (postAdded, eventType)
          }
          .runWith(Sink.seq)
          .map(_ shouldBe Vector((akkaPostAdded, "added"), (angularJsPostAdded, "added")))
      }
    }
  }
}
