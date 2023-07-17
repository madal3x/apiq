package mypack

import akka.actor.ActorSystem
import aqs._

import scalaz._
import Scalaz._
import scala.concurrent.Future

object Main extends App{
  val on = OrderNumber("123456789")
  val cn = ConsignmentNumber("987654321")
  val cc = CountryCodeIso2("NL")

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  val aqs = ApiQueueingService("http://shipments", "http://track", "http://pricing")

  val queriesValidation = (on |@| cn |@| cc).tupled

  queriesValidation match {
    case Success((on, cn, cc)) =>
      val shipReq = new Request[Shipments.type, OrderNumber](Shipments, on)
      val trackReq = new Request[Track.type, ConsignmentNumber](Track, cn)
      val pricReq = new Request[Pricing.type, CountryCodeIso2](Pricing, cc)

      val resF = aqs.executeRequests(Seq(shipReq, trackReq, pricReq))

      val shipResF: Future[Option[NonSuccessResponse \/ Seq[Product]]] = resF.map(_.get(shipReq))
      val trackResF: Future[Option[NonSuccessResponse \/ TrackingStatus]] = resF.map(_.get(trackReq))
      val pricResF: Future[Option[NonSuccessResponse \/ Double]] = resF.map(_.get(pricReq))

    case Failure(errorList) =>
      println(errorList)
  }

}
