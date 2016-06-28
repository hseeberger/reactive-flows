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

import akka.actor.{ ActorRef, Terminated }
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
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.testkit.RouteTest
import akka.http.scaladsl.testkit.TestFrameworkInterface.Scalatest
import akka.testkit.TestActor.{ AutoPilot, NoAutoPilot }
import akka.testkit.{ TestDuration, TestProbe }
import de.heikoseeberger.akkahttpcirce.CirceSupport
import java.time.LocalDateTime
import org.scalatest.{ Matchers, WordSpec }
import scala.concurrent.duration.DurationInt

class ApiSpec
    extends WordSpec
    with Matchers
    with RouteTest
    with Scalatest
    with RequestBuilding {
  import Api._
  import Flow.{ AddMessage => _, GetMessages => _, _ }
  import FlowFacade._

  private val timeout = 250.milliseconds.dilated

  "Api" should {
    "stop itself when the HTTP binding fails" in {
      val probe = TestProbe()
      val apiProps =
        Api("127.0.0.1", 18000, system.deadLetters, 100.milliseconds.dilated)
      def createAndWatch() = probe.watch(system.actorOf(apiProps))
      val api1             = createAndWatch()
      val api2             = createAndWatch()
      probe.expectMsgPF() {
        case Terminated(actor) if actor == api1 || actor == api2 => ()
      }
    }
  }

  "Api.route" should {
    "respond with PermanentRedirect to index.html upon a 'GET /'" in {
      Get() ~> route(system.deadLetters, timeout) ~> check {
        status shouldBe PermanentRedirect
        header[Location] shouldBe Some(Location(Uri("index.html")))
      }
    }

    "respond with OK upon a 'GET /test.html'" in {
      Get("/test.html") ~> route(system.deadLetters, timeout) ~> check {
        status shouldBe OK
        responseAs[String].trim shouldBe "test"
      }
    }

    // Attention: Don't move these up, else the above test will fail due to content negotiation!
    import CirceSupport._
    import io.circe.generic.auto._
    import io.circe.java8.time._

    "ask FlowFacade GetFlows and respond with OK upon a 'GET /flows'" in {
      val flowFacade = TestProbe()
      val flows =
        Set(FlowDesc("akka", "Akka"), FlowDesc("angularjs", "AngularJS"))
      flowFacade.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any) =
          msg match {
            case FlowFacade.GetFlows =>
              sender ! Flows(flows)
              NoAutoPilot
          }
      })
      Get("/flows") ~> route(flowFacade.ref, timeout) ~> check {
        status shouldBe OK
        responseAs[Set[FlowDesc]] shouldBe flows
      }
    }

    "ask FlowFacade AddFlow and respond with BadRequest upon a 'POST /flows' with an empty label" in {
      val flowFacade = TestProbe()
      val emptyLabel = "empty label"
      flowFacade.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any) =
          msg match {
            case FlowFacade.AddFlow("") =>
              sender ! BadCommand(emptyLabel)
              NoAutoPilot
          }
      })
      Post("/flows", AddFlow("")) ~> route(flowFacade.ref, timeout) ~> check {
        status shouldBe BadRequest
        responseAs[BadCommand] shouldBe BadCommand(emptyLabel)
      }
    }

    "ask FlowFacade AddFlow and respond with Conflict upon a 'POST /flows' with an existing flow" in {
      val flowFacade = TestProbe()
      val flowExists = FlowExists(FlowDesc("akka", "Akka"))
      flowFacade.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any) =
          msg match {
            case FlowFacade.AddFlow("Akka") =>
              sender ! flowExists
              NoAutoPilot
          }
      })
      Post("/flows", AddFlow("Akka")) ~> route(flowFacade.ref, timeout) ~> check {
        status shouldBe Conflict
        responseAs[FlowExists] shouldBe flowExists
      }
    }

    "ask FlowFacade AddFlow and respond with Created upon a 'POST /flows'" in {
      val flowFacade = TestProbe()
      val flowAdded  = FlowAdded(FlowDesc("akka", "Akka"))
      flowFacade.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any) =
          msg match {
            case AddFlow("Akka") =>
              sender ! flowAdded
              NoAutoPilot
          }
      })
      Post("/flows", AddFlow("Akka")) ~> route(flowFacade.ref, timeout) ~> check {
        status shouldBe Created
        responseAs[FlowAdded] shouldBe flowAdded
      }
    }

    "ask FlowFacade RemoveFlow and respond with NotFound upon a 'DELETE /flows/name' with an unknown name" in {
      val flowFacade  = TestProbe()
      val flowUnknown = FlowUnknown("unknown")
      flowFacade.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any) =
          msg match {
            case FlowFacade.RemoveFlow("unknown") =>
              sender ! flowUnknown
              NoAutoPilot
          }
      })
      Delete("/flows/unknown") ~> route(flowFacade.ref, timeout) ~> check {
        status shouldBe NotFound
        responseAs[FlowUnknown] shouldBe flowUnknown
      }
    }

    "ask FlowFacade RemoveFlow and respond with NoContent upon a 'DELETE /flows/name'" in {
      val flowFacade  = TestProbe()
      val flowRemoved = FlowRemoved("akka")
      flowFacade.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any) =
          msg match {
            case FlowFacade.RemoveFlow("akka") =>
              sender ! flowRemoved
              NoAutoPilot
          }
      })
      Delete("/flows/akka") ~> route(flowFacade.ref, timeout) ~> check {
        status shouldBe NoContent
      }
    }

    "ask FlowFacade GetMessages and respond with NotFound upon a 'GET /flows/name/messages' with an unknown name" in {
      val flowFacade  = TestProbe()
      val flowUnknown = FlowUnknown("unknown")
      flowFacade.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any) =
          msg match {
            case FlowFacade.GetMessages("unknown", 1, 1) =>
              sender ! flowUnknown
              NoAutoPilot
          }
      })
      Get("/flows/unknown/messages?id=1") ~> route(flowFacade.ref, timeout) ~> check {
        status shouldBe NotFound
        responseAs[FlowUnknown] shouldBe flowUnknown
      }
    }

    "ask FlowFacade GetMessages and respond with OK upon a 'GET /flows/name/messages'" in {
      val flowFacade = TestProbe()
      val messages   = Vector(Message(1, "m1", now()), Message(0, "m0", now()))
      flowFacade.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any) =
          msg match {
            case FlowFacade.GetMessages("akka", Long.MaxValue, 2) =>
              sender ! Messages(messages)
              NoAutoPilot
          }
      })
      Get("/flows/akka/messages?count=2") ~> route(flowFacade.ref, timeout) ~> check {
        status shouldBe OK
        responseAs[Seq[Message]] shouldBe messages
      }
    }

    "ask FlowFacade AddMessages and respond with BadRequest upon a 'POST /flows/name/messages' with an empty text" in {
      val flowFacade = TestProbe()
      val emptyText  = "empty text"
      flowFacade.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any) =
          msg match {
            case FlowFacade.AddMessage("akka", "") =>
              sender ! BadCommand(emptyText)
              NoAutoPilot
          }
      })
      val request = Post("/flows/akka/messages", AddMessageRequest(""))
      request ~> route(flowFacade.ref, timeout) ~> check {
        status shouldBe BadRequest
        responseAs[BadCommand] shouldBe BadCommand(emptyText)
      }
    }

    "ask FlowFacade AddMessages and respond with NotFound upon a 'POST /flows/name/messages' with an unknown name" in {
      val flowFacade  = TestProbe()
      val flowUnknown = FlowUnknown("unknown")
      flowFacade.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any) =
          msg match {
            case FlowFacade.AddMessage("unknown", "text") =>
              sender ! flowUnknown
              NoAutoPilot
          }
      })
      val request = Post("/flows/unknown/messages", AddMessageRequest("text"))
      request ~> route(flowFacade.ref, timeout) ~> check {
        status shouldBe NotFound
        responseAs[FlowUnknown] shouldBe flowUnknown
      }
    }

    "ask FlowFacade AddMessages and respond with NoContent upon a 'POST /flows/name/messages'" in {
      val flowFacade   = TestProbe()
      val messageAdded = MessageAdded("akka", Message(0, "text", now()))
      flowFacade.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any) =
          msg match {
            case FlowFacade.AddMessage("akka", "text") =>
              sender ! messageAdded
              NoAutoPilot
          }
      })
      val request = Post("/flows/akka/messages", AddMessageRequest("text"))
      request ~> route(flowFacade.ref, timeout) ~> check {
        status shouldBe Created
        responseAs[MessageAdded] shouldBe messageAdded
      }
    }
  }

  private def now() = LocalDateTime.now().withNano(0)
}
