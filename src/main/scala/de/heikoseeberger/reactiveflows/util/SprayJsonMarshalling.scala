/*
 * Copyright 2014 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.reactiveflows
package util

import akka.http.marshalling.{ PredefinedToEntityMarshallers, ToEntityMarshaller => TEM }
import akka.http.model.{ ContentTypeRange, MediaRange, MediaTypes }
import akka.http.unmarshalling.Unmarshaller.UnsupportedContentTypeException
import akka.http.unmarshalling.{ PredefinedFromEntityUnmarshallers, FromEntityUnmarshaller => FEUM }
import akka.http.util.FastFuture
import akka.stream.FlowMaterializer
import scala.concurrent.ExecutionContext
import spray.json.{ JsonParser, JsonPrinter, PrettyPrinter, RootJsonReader, RootJsonWriter }

trait SprayJsonMarshalling {

  implicit def feum[A](implicit reader: RootJsonReader[A], m: FlowMaterializer, ec: ExecutionContext): FEUM[A] =
    PredefinedFromEntityUnmarshallers.stringUnmarshaller.flatMapWithInput { (entity, s) =>
      if (entity.contentType.mediaType == MediaTypes.`application/json`)
        FastFuture.successful(reader.read(JsonParser(s)))
      else
        FastFuture.failed(
          UnsupportedContentTypeException(ContentTypeRange(MediaRange(MediaTypes.`application/json`)))
        )
    }

  implicit def tem[A](implicit writer: RootJsonWriter[A], printer: JsonPrinter = PrettyPrinter): TEM[A] = {
    val stringMarshaller = PredefinedToEntityMarshallers.stringMarshaller(MediaTypes.`application/json`)
    stringMarshaller.compose(printer).compose(writer.write)
  }
}
