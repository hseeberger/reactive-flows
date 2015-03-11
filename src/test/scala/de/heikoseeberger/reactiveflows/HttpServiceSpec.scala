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

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{ RouteTest, TestFrameworkInterface }
import akka.stream.scaladsl.Source
import akka.testkit.{ TestActor, TestProbe }
import de.heikoseeberger.akkahttpjsonspray.SprayJsonMarshalling
import de.heikoseeberger.akkasse.ServerSentEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.scalatest.{ Matchers, WordSpec }
import spray.json.pimpString

class HttpServiceSpec extends WordSpec with Matchers with RouteTest with TestFrameworkInterface.Scalatest
    with RequestBuilding with SprayJsonMarshalling with JsonProtocol {

  import HttpService._
  val settings = Settings(system)
  import settings.httpService._

  "A HttpService" should {

    "send itself a Shutdown upon a 'DELETE /' and respond with OK" in {
      val httpService = TestProbe()
      val flowFacade = TestProbe()
      val request = Delete()
      request ~> route(httpService.ref, selfTimeout, flowFacade.ref, flowFacadeTimeout) ~> check {
        response.status shouldBe StatusCodes.OK
      }
      httpService.expectMsg(Shutdown)
    }

    "respond with OK and index.html upon a 'GET /'" in {
      val httpService = TestProbe()
      val flowFacade = TestProbe()
      val request = Get()
      request ~> route(httpService.ref, selfTimeout, flowFacade.ref, flowFacadeTimeout) ~> check {
        response.status shouldBe StatusCodes.OK
        responseAs[String].trim shouldBe "test"
      }
    }

    "respond with OK and index.html upon a 'GET /index.html'" in {
      val httpService = TestProbe()
      val flowFacade = TestProbe()
      val request = Get("/index.html")
      request ~> route(httpService.ref, selfTimeout, flowFacade.ref, flowFacadeTimeout) ~> check {
        response.status shouldBe StatusCodes.OK
        responseAs[String].trim shouldBe "test"
      }
    }

    "ask the FlowFacade GetFlows and respond with OK with the proper payload upon a 'GET /flows'" in {
      val httpService = TestProbe()
      val flowFacade = TestProbe()
      flowFacade.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any) = msg match {
          case FlowFacade.GetFlows =>
            sender ! List(
              FlowFacade.FlowInfo("akka", "Akka"),
              FlowFacade.FlowInfo("angularjs", "AngularJS")
            )
            TestActor.NoAutoPilot
        }
      })
      val request = Get("/flows")
      request ~> route(httpService.ref, selfTimeout, flowFacade.ref, flowFacadeTimeout) ~> check {
        response.status shouldBe StatusCodes.OK
        responseAs[String].parseJson shouldBe
          """|[
             |  { "name": "akka", "label": "Akka" },
             |  { "name": "angularjs", "label": "AngularJS" }
             |]""".stripMargin.parseJson
      }
    }

    "ask the FlowFacade AddFlow and respond with Created with the proper payload upon a 'POST /flows' for an unknown Flow" in {
      val httpService = TestProbe()
      val flowFacade = TestProbe()
      flowFacade.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any) = msg match {
          case FlowFacade.AddFlow("Akka") =>
            sender ! FlowFacade.FlowAdded(FlowFacade.FlowInfo("akka", "Akka"))
            TestActor.NoAutoPilot
        }
      })
      val request = Post("/flows", AddFlowRequest("Akka"))
      request ~> route(httpService.ref, selfTimeout, flowFacade.ref, flowFacadeTimeout) ~> check {
        response.status shouldBe StatusCodes.Created
        responseAs[String].parseJson shouldBe
          """|{
             |  "flowInfo": { "name": "akka", "label": "Akka" }
             |}""".stripMargin.parseJson
      }
    }

    "ask the FlowFacade AddFlow and respond with Conflict with the proper payload upon a 'POST /flows' for an existing Flow" in {
      val httpService = TestProbe()
      val flowFacade = TestProbe()
      flowFacade.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any) = msg match {
          case FlowFacade.AddFlow("Akka") =>
            sender ! FlowFacade.FlowExists("Akka")
            TestActor.NoAutoPilot
        }
      })
      val request = Post("/flows", AddFlowRequest("Akka"))
      request ~> route(httpService.ref, selfTimeout, flowFacade.ref, flowFacadeTimeout) ~> check {
        response.status shouldBe StatusCodes.Conflict
        responseAs[String].parseJson shouldBe
          """|{ "label": "Akka" }""".stripMargin.parseJson
      }
    }

    "ask the FlowFacade RemoveFlow and respond with NoContent with the proper payload upon a 'DELETE /flows/akka' for an existing Flow" in {
      val httpService = TestProbe()
      val flowFacade = TestProbe()
      flowFacade.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any) = msg match {
          case FlowFacade.RemoveFlow("akka") =>
            sender ! FlowFacade.FlowRemoved("akka")
            TestActor.NoAutoPilot
        }
      })
      val request = Delete("/flows/akka")
      request ~> route(httpService.ref, selfTimeout, flowFacade.ref, flowFacadeTimeout) ~> check {
        response.status shouldBe StatusCodes.NoContent
      }
    }

    "ask the FlowFacade RemoveFlow and respond with NotFound with the proper payload upon a 'DELETE /flows/unknown' for an unknown Flow" in {
      val httpService = TestProbe()
      val flowFacade = TestProbe()
      flowFacade.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any) = msg match {
          case FlowFacade.RemoveFlow("unknown") =>
            sender ! FlowFacade.UnknownFlow("unknown")
            TestActor.NoAutoPilot
        }
      })
      val request = Delete("/flows/unknown")
      request ~> route(httpService.ref, selfTimeout, flowFacade.ref, flowFacadeTimeout) ~> check {
        response.status shouldBe StatusCodes.NotFound
      }
    }

    "ask the FlowFacade GetMessages and respond with OK with the proper payload upon a 'GET /flows/akka/messages' for an existing Flow" in {
      val httpService = TestProbe()
      val flowFacade = TestProbe()
      flowFacade.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any) = msg match {
          case FlowFacade.GetMessages("akka") =>
            sender ! Seq.empty
            TestActor.NoAutoPilot
        }
      })
      val request = Get("/flows/akka/messages")
      request ~> route(httpService.ref, selfTimeout, flowFacade.ref, flowFacadeTimeout) ~> check {
        response.status shouldBe StatusCodes.OK
      }
    }

    "ask the FlowFacade GetMessages and respond with NotFound with the proper payload upon a 'GET /flows/unknown/messages' for an unknown Flow" in {
      val httpService = TestProbe()
      val flowFacade = TestProbe()
      flowFacade.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any) = msg match {
          case FlowFacade.GetMessages("unknown") =>
            sender ! FlowFacade.UnknownFlow("unknown")
            TestActor.NoAutoPilot
        }
      })
      val request = Get("/flows/unknown/messages")
      request ~> route(httpService.ref, selfTimeout, flowFacade.ref, flowFacadeTimeout) ~> check {
        response.status shouldBe StatusCodes.NotFound
      }
    }

    "ask itself CreateFlowEventSource and respond with OK and an SSE stream upon a GET for '/flow-events'" in {
      val httpService = TestProbe()
      httpService.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any) = msg match {
          case CreateFlowEventSource =>
            sender ! Source(List(
              FlowFacade.FlowAdded(FlowFacade.FlowInfo("akka", "Akka")),
              FlowFacade.FlowRemoved("angularjs")
            )).map(ServerSentEventProtocol.flowEventToServerSentEvent)
            TestActor.NoAutoPilot
        }
      })
      val flowFacade = TestProbe()
      val request = Get("/flow-events")
      request ~> route(httpService.ref, selfTimeout, flowFacade.ref, flowFacadeTimeout) ~> check {
        response.status shouldBe StatusCodes.OK
        val expected = """|event:added
                          |data:{
                          |data:  "name": "akka",
                          |data:  "label": "Akka"
                          |data:}
                          |
                          |event:removed
                          |data:angularjs
                          |
                          |""".stripMargin
        responseAs[String] shouldBe expected
      }
      httpService.expectMsg(CreateFlowEventSource)
    }
  }

  "respond with a Source upon a CreateFlowEventSource" in {
    val mediator = TestProbe()
    val flowFacade = TestProbe()
    val flowEventSource = Source.empty[ServerSentEvent]
    val httpService = actor(new HttpService(interface, port, selfTimeout, flowFacade.ref, flowFacadeTimeout, mediator.ref) {
      override protected def serveHttp() = ()
      override protected def createFlowEventSource() = flowEventSource
    })

    val sender = TestProbe()
    implicit val senderRef = sender.ref

    httpService ! CreateFlowEventSource
    sender.expectMsg(flowEventSource)
  }

  "ask itself CreateMessageEventSource and respond with OK and an SSE stream upon a GET for '/message-events'" in {
    val time = LocalDateTime.from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse("2015-04-14T19:20:21"))
    val httpService = TestProbe()
    httpService.setAutoPilot(new TestActor.AutoPilot {
      def run(sender: ActorRef, msg: Any) = msg match {
        case CreateMessageEventSource =>
          sender ! Source(List(
            Flow.MessageAdded("akka", Flow.Message("Akka rocks!", time)),
            Flow.MessageAdded("angularjs", Flow.Message("AngularJS rocks!", time))
          )).map(ServerSentEventProtocol.messageEventToServerSentEvent)
          TestActor.NoAutoPilot
      }
    })
    val flowFacade = TestProbe()
    val request = Get("/message-events")
    request ~> route(httpService.ref, selfTimeout, flowFacade.ref, flowFacadeTimeout) ~> check {
      response.status shouldBe StatusCodes.OK
      val expected = """|event:added
                        |data:{
                        |data:  "flowName": "akka",
                        |data:  "message": {
                        |data:    "text": "Akka rocks!",
                        |data:    "dateTime": "2015-04-14 19:20:21"
                        |data:  }
                        |data:}
                        |
                        |event:added
                        |data:{
                        |data:  "flowName": "angularjs",
                        |data:  "message": {
                        |data:    "text": "AngularJS rocks!",
                        |data:    "dateTime": "2015-04-14 19:20:21"
                        |data:  }
                        |data:}
                        |
                        |""".stripMargin
      responseAs[String] shouldBe expected
    }
    httpService.expectMsg(CreateMessageEventSource)
  }

  "respond with a Source upon a CreateMessageEventSource" in {
    val mediator = TestProbe()
    val flowFacade = TestProbe()
    val messageEventSource = Source.empty[ServerSentEvent]
    val httpService = actor(new HttpService(interface, port, selfTimeout, flowFacade.ref, flowFacadeTimeout, mediator.ref) {
      override protected def serveHttp() = ()
      override protected def createMessageEventSource() = messageEventSource
    })

    val sender = TestProbe()
    implicit val senderRef = sender.ref

    httpService ! CreateMessageEventSource
    sender.expectMsg(messageEventSource)
  }
}
