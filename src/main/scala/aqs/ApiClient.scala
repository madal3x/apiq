package aqs

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.util.ByteString

import spray.json._

import scalaz._
import Scalaz._

import scala.concurrent.{ExecutionContext, Future}

private[aqs] abstract class ApiClient(implicit system: ActorSystem, executionContext: ExecutionContext) {
  val requestToResponse: HttpRequest => Future[HttpResponse]

  implicit val materializer = ActorMaterializer()

  def execReqs[I <: Identifier, Q <: Query, Res](reqs: Seq[Request[I, Q]], apiUrl: String)
                                                (implicit queryDep: QueryDep[I, Q], responseDep: ResponseDep[I, Res], json: JsonReader[Map[Q, Res]], qpv: QueryParamValue[Q]): Future[Seq[(Request[I, Q], NonSuccessResponse \/ Res)]] = {

    val q = reqs
      .map(_.query)
      .map(qpv.queryParamValue)
      .mkString(",")

    val uri = s"$apiUrl?q=$q"

    val responseMap = requestToResponse(HttpRequest(uri = uri)) flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Unmarshal(entity)
          .to[String]
          .map(jsonString =>
            jsonString.parseJson.convertTo[Map[Q, Res]])
          .map(_.right)
      case HttpResponse(statusCode, _, entity, _) =>
        entity
          .dataBytes
          .runFold(ByteString(""))(_ ++ _)
          .map(_.utf8String)
          .map(bodyString =>
            NonSuccessResponse(statusCode.intValue(), bodyString))
          .map(_.left)
    }

    responseMap map {
      case \/-(rmap) =>
        reqs.map(r => (r, rmap(r.query).right))
      case -\/(err) =>
        reqs.map(r => (r, err.left))
    }
  }
}

object ApiClient {
  def apply(implicit system: ActorSystem, executionContext: ExecutionContext) = new ApiClient() {
    val requestToResponse = (req: HttpRequest) => Http().singleRequest(request = req)
  }
}