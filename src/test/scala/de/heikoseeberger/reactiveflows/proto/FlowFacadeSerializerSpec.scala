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

final class FlowFacadeSerializerSpec extends WordSpec with Matchers {
  import FlowFacade._

  private val serializer = new FlowFacadeSerializer
  private val desc       = FlowDesc("name", "label")
  private val desc2      = FlowDesc("name2", "label2")

  "FlowFacadeSerializer" should {
    "serialize and deserialize GetFlows" in {
      val (manifest, binary) = serialize(GetFlows)
      serializer.fromBinary(binary, manifest) shouldBe GetFlows
    }

    "serialize and deserialize Flows" in {
      val o                  = Flows(Set(desc, desc2))
      val (manifest, binary) = serialize(o)
      serializer.fromBinary(binary, manifest) shouldBe o
    }

    "serialize and deserialize AddFlow" in {
      val o                  = AddFlow("label")
      val (manifest, binary) = serialize(o)
      serializer.fromBinary(binary, manifest) shouldBe o
    }

    "serialize and deserialize FlowAdded" in {
      val o                  = FlowAdded(desc)
      val (manifest, binary) = serialize(o)
      serializer.fromBinary(binary, manifest) shouldBe o
    }

    "serialize and deserialize FlowExists" in {
      val o                  = FlowExists(desc)
      val (manifest, binary) = serialize(o)
      serializer.fromBinary(binary, manifest) shouldBe o
    }

    "serialize and deserialize RemoveFlow" in {
      val o                  = RemoveFlow("name")
      val (manifest, binary) = serialize(o)
      serializer.fromBinary(binary, manifest) shouldBe o
    }

    "serialize and deserialize FlowRemoved" in {
      val o                  = FlowRemoved("name")
      val (manifest, binary) = serialize(o)
      serializer.fromBinary(binary, manifest) shouldBe o
    }

    "serialize and deserialize FlowUnknown" in {
      val o                  = FlowUnknown("name")
      val (manifest, binary) = serialize(o)
      serializer.fromBinary(binary, manifest) shouldBe o
    }

    "serialize and deserialize GetMessages" in {
      val o                  = GetMessages("name", 0, 1)
      val (manifest, binary) = serialize(o)
      serializer.fromBinary(binary, manifest) shouldBe o
    }

    "serialize and deserialize MessageAdd" in {
      val o                  = AddMessage("name", "text")
      val (manifest, binary) = serialize(o)
      serializer.fromBinary(binary, manifest) shouldBe o
    }
  }

  private def serialize(o: AnyRef) = (serializer.manifest(o), serializer.toBinary(o))
}
