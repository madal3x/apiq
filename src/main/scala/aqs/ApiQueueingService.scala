package aqs

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._

import scalaz._
import Scalaz._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.postfixOps
import JsonSupport._
import QueryDep._
import ResponseDep._
import QueryParamValue._

/**
  * Example:
  *
  * {{{
  * import aqs._
  * import scalaz._, Scalaz._
  * import akka.actor.ActorSystem
  *
  * val on = OrderNumber("123456789")
  * val cn = ConsignmentNumber("987654321")
  * val cc = CountryCodeIso2("NL")
  *
  * implicit val system = ActorSystem()
  * implicit val executionContext = system.dispatcher
  *
  * val aqs = ApiQueueingService("http://shipments", "http://track", "http://pricing")
  *
  * val queriesValidation = (on |@| cn |@| cc).tupled
  *
  * queriesValidation match {
  *   case Success((on, cn, cc)) =>
  *     val shipReq = new Request[Shipments.type, OrderNumber](Shipments, on)
  *     val trackReq = new Request[Track.type, ConsignmentNumber](Track, cn)
  *     val pricReq = new Request[Pricing.type, CountryCodeIso2](Pricing, cc)
  *
  *     val resF = aqs.executeRequests(Seq(shipReq, trackReq, pricReq))
  *
  *     val shipResF: Future[Option[NonSuccessResponse \/ Seq[Product]]] = resF.map(_.get(shipReq))
  *     val trackResF: Future[Option[NonSuccessResponse \/ TrackingStatus]] = resF.map(_.get(trackReq))
  *     val pricResF: Future[Option[NonSuccessResponse \/ Double]] = resF.map(_.get(pricReq))
  *
  *     case Failure(queryValidationErrors) =>
  *       println(queryValidationErrors)
  * }
  *
  * }}}
  *
  *
  * @param shipmentsUrl
  * @param trackUrl
  * @param pricingUrl
  * @param system
  * @param executionContext
  */
abstract class ApiQueueingService private[aqs] (shipmentsUrl: String, trackUrl: String, pricingUrl: String)
                                               (implicit system: ActorSystem, executionContext: ExecutionContext) {

  private val OverflowStrategy = akka.stream.OverflowStrategy.fail
  private val Cap = 5
  private val SendEvery = 5 seconds
  private val NrApis = 3

  protected val api: ApiClient

  private implicit val materializer = ActorMaterializer()

  type ResponseMap[I <: Identifier, Q <: Query] = Map[Request[I, Q], Promise[\/[NonSuccessResponse, Any]]]

  case class ReqMessage[I <: Identifier, Q <: Query](request: Request[I, Q], responseMap: ResponseMap[I, Q])
  case class ReqResMessage[I <: Identifier, Q <: Query](request: Request[I, Q], response: \/[NonSuccessResponse, Any], responseMap: ResponseMap[I, Q])

  private def toReqResMessage[I <: Identifier, Q <: Query, Res](messages: Seq[ReqMessage[I, Q]]) =
    (req: Request[I, Q], res: \/[NonSuccessResponse, Res]) =>
      ReqResMessage(req, res, messages.find(_.request == req).map(_.responseMap).get)

  private val resolveResPromisesSink = Sink.foreach[Seq[ReqResMessage[Identifier, Query]]](
    resmessages => resmessages.foreach(m => m.responseMap(m.request).success(m.response)))

  private def executeReqMessages(ms: Seq[ReqMessage[Identifier, Query]]) = ms match {
    case messages @ Seq(ReqMessage(Request(Shipments, _), _), _ *) =>
      api
        .execReqs(messages.map(_.request).asInstanceOf[Seq[Request[Shipments.type, OrderNumber]]], shipmentsUrl)
        .map(_.map(toReqResMessage(messages).tupled))
    case messages @ Seq(ReqMessage(Request(Track, _), responseMap), _ *) =>
      api
        .execReqs(messages.map(_.request).asInstanceOf[Seq[Request[Track.type, ConsignmentNumber]]], trackUrl)
        .map(_.map(toReqResMessage(messages).tupled))
    case messages @ Seq(ReqMessage(Request(Pricing, _), responseMap), _ *) =>
      api
        .execReqs(messages.map(_.request).asInstanceOf[Seq[Request[Pricing.type, CountryCodeIso2]]], pricingUrl)
        .map(_.map(toReqResMessage(messages).tupled))
  }

  private val sourceRef =
    Source.actorRef[ReqMessage[Identifier, Query]](100, OverflowStrategy)
      .groupBy(NrApis, _.request.identifier)
      .groupedWithin(Cap, SendEvery)
      .mapAsyncUnordered(3)(executeReqMessages)
      .mergeSubstreams
      .to(resolveResPromisesSink)
      .run()

  def executeRequests(reqs: Seq[Request[Identifier, Query]]): Future[ApiResponseMap] = {
    val responseMap: ResponseMap[Identifier, Query] =
      reqs
        .map((_, Promise[\/[NonSuccessResponse, Any]]))
        .toMap

    reqs.foreach(req => sourceRef ! ReqMessage(req, responseMap))

    val responseMapF = Future.sequence(
      responseMap
        .mapValues(_.future)
        .toSeq
        .map{ case (req, resF) =>
          resF.map(res =>
            (req, res))}
    ).map(_.toMap)

    responseMapF.map(rmap =>
      new ApiResponseMap(rmap))
  }
}

object ApiQueueingService {
  def apply(shipmentsUrl: String, trackUrl: String, pricingUrl: String)
           (implicit system: ActorSystem, executionContext: ExecutionContext) = new ApiQueueingService(shipmentsUrl, trackUrl, pricingUrl) {

    val api = ApiClient(system, executionContext)
  }
}