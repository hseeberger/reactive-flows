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

import akka.actor.{ ExtendedActorSystem, Extension, ExtensionKey }
import akka.cluster.Cluster
import akka.contrib.datareplication.{ DataReplication, ORSet, Replicator }
import akka.contrib.pattern.{ DistributedPubSubExtension, DistributedPubSubMediator }
import akka.pattern.ask
import akka.util.Timeout
import java.net.URLEncoder
import scala.concurrent.{ ExecutionContext, Future }
import spray.json.DefaultJsonProtocol

object FlowRepository extends ExtensionKey[FlowRepository] {

  sealed trait Event

  case class FlowData(name: String, label: String)
  object FlowData extends DefaultJsonProtocol { implicit val format = jsonFormat2(apply) }

  case class AddFlowRequest(label: String)
  object AddFlowRequest extends DefaultJsonProtocol { implicit val format = jsonFormat1(apply) }

  sealed trait AddFlowReponse
  case class FlowAdded(flowData: FlowData) extends AddFlowReponse with Event
  case class FlowAlreadyRegistered(name: String)
    extends IllegalStateException(s"Flow [$name] already exists!")
    with AddFlowReponse

  sealed trait RemoveFlowReponse
  case class FlowRemoved(name: String) extends RemoveFlowReponse with Event
  case class UnknownFlow(name: String)
    extends IllegalStateException(s"Flow [$name] doesn't exist!")
    with RemoveFlowReponse

  val ReplicatorKey = "flow-data"

  val EventKey = "flow-repository-events"
}

class FlowRepository(system: ExtendedActorSystem) extends Extension {

  import FlowRepository._

  private implicit val node = Cluster(system)

  private val replicator = DataReplication(system).replicator

  private val mediator = DistributedPubSubExtension(system).mediator

  private val settings = Settings(system).flowRepository

  def findAll(implicit timeout: Timeout, ec: ExecutionContext): Future[Set[FlowData]] = {
    replicator
      .ask(get)
      .mapTo[Replicator.GetResponse]
      .flatMap {
        case Replicator.GetSuccess(ReplicatorKey, flows: ORSet, _) =>
          Future.successful(flows.value.asInstanceOf[Set[FlowData]])
        case Replicator.NotFound(ReplicatorKey, _) =>
          Future.successful(Set.empty[FlowData])
        case other =>
          Future.failed(new Exception(s"Error getting all flow data: $other"))
      }
  }

  def find(flowName: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[FlowData]] = {
    replicator
      .ask(get)
      .mapTo[Replicator.GetResponse]
      .flatMap {
        case Replicator.GetSuccess(ReplicatorKey, flows: ORSet, _) =>
          Future.successful(flows.value.asInstanceOf[Set[FlowData]].find(_.name == flowName))
        case Replicator.NotFound(ReplicatorKey, _) =>
          Future.successful(None)
        case other =>
          Future.failed(new Exception(s"Error getting flow data for [$flowName]: $other"))
      }
  }

  def add(request: AddFlowRequest)(implicit timeout: Timeout, ec: ExecutionContext): Future[AddFlowReponse] = {
    val flowData = FlowData(URLEncoder.encode(request.label.toLowerCase, "UTF-8"), request.label)
    def checkAndAdd(flowDataSet: ORSet) =
      if (flowDataSet.contains(flowData))
        throw FlowAlreadyRegistered(flowData.name)
      else
        flowDataSet + flowData
    replicator
      .ask(update(checkAndAdd))
      .mapTo[Replicator.UpdateResponse]
      .flatMap {
        case Replicator.UpdateSuccess(ReplicatorKey, _) =>
          val flowAdded = FlowAdded(flowData)
          mediator ! DistributedPubSubMediator.Publish(EventKey, flowAdded)
          Future.successful(flowAdded)
        case Replicator.ModifyFailure(ReplicatorKey, _, flowAlreadyRegistered: FlowAlreadyRegistered, _) =>
          Future.successful(flowAlreadyRegistered)
        case other =>
          Future.failed(new Exception(s"Error adding flow [${flowData.name}]: $other"))
      }
  }

  def remove(flowName: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[RemoveFlowReponse] = {
    def checkAndRemove(flows: ORSet) = {
      val flowData = flows.value.asInstanceOf[Set[FlowData]].find(_.name == flowName)
      flowData.fold(throw UnknownFlow(flowName))(flows - _)
    }
    replicator
      .ask(update(checkAndRemove))
      .mapTo[Replicator.UpdateResponse]
      .flatMap {
        case Replicator.UpdateSuccess(ReplicatorKey, _) =>
          val flowRemoved = FlowRemoved(flowName)
          mediator ! DistributedPubSubMediator.Publish(EventKey, flowRemoved)
          Future.successful(flowRemoved)
        case Replicator.ModifyFailure(ReplicatorKey, _, unknownFlow: UnknownFlow, _) =>
          Future.successful(unknownFlow)
        case other =>
          Future.failed(new Exception(s"Error removing flow [$flowName]: $other"))
      }
  }

  private def get: Replicator.Get =
    Replicator.Get(ReplicatorKey, Replicator.ReadQuorum(settings.readTimeout))

  private def update(modify: ORSet => ORSet): Replicator.Update[ORSet] =
    Replicator.Update(
      ReplicatorKey,
      ORSet.empty,
      Replicator.ReadQuorum(settings.readTimeout),
      Replicator.WriteQuorum(settings.writeTimeout)
    )(modify)
}
