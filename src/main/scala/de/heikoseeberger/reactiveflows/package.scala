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

package de.heikoseeberger

import akka.actor.ActorContext
import java.time.{ Duration => JavaDuration }
import scala.concurrent.duration.{ FiniteDuration, NANOSECONDS }
import scala.reflect.{ classTag, ClassTag }

package object reactiveflows {

  type Traversable[+A] = scala.collection.immutable.Traversable[A]
  type Iterable[+A]    = scala.collection.immutable.Iterable[A]
  type Seq[+A]         = scala.collection.immutable.Seq[A]
  type IndexedSeq[+A]  = scala.collection.immutable.IndexedSeq[A]

  final case class BadCommand(command: String)

  def badCommand(command: String)(implicit context: ActorContext): Unit =
    context.sender() ! BadCommand(command)

  implicit def javaDurationToScala(duration: JavaDuration): FiniteDuration =
    FiniteDuration(duration.toNanos, NANOSECONDS)

  def className[A: ClassTag]: String =
    classTag[A].runtimeClass.getSimpleName
}
