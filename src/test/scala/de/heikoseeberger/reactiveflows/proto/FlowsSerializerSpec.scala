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
package proto

import org.scalatest.{ Matchers, WordSpec }

final class FlowsSerializerSpec extends WordSpec with Matchers {
  import Flows._

  private val serializer = new FlowsSerializer
  private val desc       = FlowDesc("name", "label")
  private val desc2      = FlowDesc("name2", "label2")

  "FlowFacadeSerializer" should {

    "serialize and deserialize AddFlow" in {
      val message            = AddFlow("label")
      val (manifest, binary) = serialize(message)
      serializer.fromBinary(binary, manifest) shouldBe message
    }

    "serialize and deserialize FlowAdded" in {
      val message            = FlowAdded(desc)
      val (manifest, binary) = serialize(message)
      serializer.fromBinary(binary, manifest) shouldBe message
    }

    "serialize and deserialize FlowExists" in {
      val message            = FlowExists(desc)
      val (manifest, binary) = serialize(message)
      serializer.fromBinary(binary, manifest) shouldBe message
    }

    "serialize and deserialize RemoveFlow" in {
      val message            = RemoveFlow("name")
      val (manifest, binary) = serialize(message)
      serializer.fromBinary(binary, manifest) shouldBe message
    }

    "serialize and deserialize FlowRemoved" in {
      val message            = FlowRemoved("name")
      val (manifest, binary) = serialize(message)
      serializer.fromBinary(binary, manifest) shouldBe message
    }

    "serialize and deserialize FlowUnknown" in {
      val message            = FlowUnknown("name")
      val (manifest, binary) = serialize(message)
      serializer.fromBinary(binary, manifest) shouldBe message
    }
  }

  private def serialize(o: AnyRef) = (serializer.manifest(o), serializer.toBinary(o))
}
