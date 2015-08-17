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

import cats.data.Xor
import io.circe.{ Decoder, Encoder, Json }
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object CirceCodec {

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  implicit val localDateTimeEncoder: Encoder[LocalDateTime] =
    Encoder.instance(dateTime => Json.string(formatter.format(dateTime)))

  implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    Decoder[String].flatMap(s => Decoder.instance(_ => Xor.fromTryCatch(LocalDateTime.from(formatter.parse(s)))))
}
