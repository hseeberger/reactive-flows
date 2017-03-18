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

import com.trueaccord.scalapb.{ GeneratedMessage, GeneratedMessageCompanion, Message }

package object proto {

  implicit class MessageOps[A <: GeneratedMessage with Message[A]](val message: A) extends AnyRef {
    def toBinary: Array[Byte] =
      message.companion.asInstanceOf[GeneratedMessageCompanion[A]].toByteArray(message)
  }
}
