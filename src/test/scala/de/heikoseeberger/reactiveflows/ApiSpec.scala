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

import akka.actor.Terminated
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes.{ OK, PermanentRedirect }
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.testkit.RouteTest
import akka.http.scaladsl.testkit.TestFrameworkInterface.Scalatest
import akka.testkit.TestProbe
import org.scalatest.{ Matchers, WordSpec }

class ApiSpec
    extends WordSpec
    with Matchers
    with RouteTest
    with Scalatest
    with RequestBuilding {
  import Api._

  "Api" should {
    "stop itself when the HTTP binding fails" in {
      val probe            = TestProbe()
      val apiProps         = Api("127.0.0.1", 18000)
      def createAndWatch() = probe.watch(system.actorOf(apiProps))
      val api1             = createAndWatch()
      val api2             = createAndWatch()
      probe.expectMsgPF() {
        case Terminated(actor) if actor == api1 || actor == api2 => ()
      }
    }
  }

  "route" should {
    "respond with PermanentRedirect to index.html upon a 'GET /'" in {
      Get() ~> route ~> check {
        status shouldBe PermanentRedirect
        header[Location] shouldBe Some(Location(Uri("index.html")))
      }
    }

    "respond with OK and 'test' upon a 'GET /test.html'" in {
      Get("/test.html") ~> route ~> check {
        status shouldBe OK
        responseAs[String].trim shouldBe "test"
      }
    }
  }
}
