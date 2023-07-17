package aqs

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}

import scalaz._, Scalaz._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object HttpRequestUri {
  def unapply(arg: HttpRequest): Option[String] =
    Some(arg.uri.toString())
}

class ApiQueueingServiceSpec extends AsyncFlatSpec with BeforeAndAfterAll {
  val ShipmentsUrl = "shipments"
  val TrackUrl = "track"
  val PricingUrl = "pricing"

  implicit val system = ActorSystem()

  it should "make 2 calls, with a couple of duplicate queries, that return both successful and failure responses" in {
    val reqSh0 = new Request[Shipments.type, OrderNumber](Shipments, OrderNumber("100000000").toOption.get)
    val reqSh1 = new Request[Shipments.type, OrderNumber](Shipments, OrderNumber("100000001").toOption.get)
    val reqSh2 = new Request[Shipments.type, OrderNumber](Shipments, OrderNumber("100000002").toOption.get)
    val reqSh3 = new Request[Shipments.type, OrderNumber](Shipments, OrderNumber("100000003").toOption.get)
    val reqSh3c = new Request[Shipments.type, OrderNumber](Shipments, OrderNumber("100000003").toOption.get)
    val reqSh4 = new Request[Shipments.type, OrderNumber](Shipments, OrderNumber("100000004").toOption.get)
    val reqSh5 = new Request[Shipments.type, OrderNumber](Shipments, OrderNumber("100000005").toOption.get)
    val reqSh6 = new Request[Shipments.type, OrderNumber](Shipments, OrderNumber("100000006").toOption.get)

    val reqTr0 = new Request[Track.type, ConsignmentNumber](Track, ConsignmentNumber("200000000").toOption.get)
    val reqTr1 = new Request[Track.type, ConsignmentNumber](Track, ConsignmentNumber("200000001").toOption.get)
    val reqTr2 = new Request[Track.type, ConsignmentNumber](Track, ConsignmentNumber("200000002").toOption.get)
    val reqTr3 = new Request[Track.type, ConsignmentNumber](Track, ConsignmentNumber("200000003").toOption.get)
    val reqTr4 = new Request[Track.type, ConsignmentNumber](Track, ConsignmentNumber("200000004").toOption.get)
    val reqTr5 = new Request[Track.type, ConsignmentNumber](Track, ConsignmentNumber("200000005").toOption.get)
    val reqTr6 = new Request[Track.type, ConsignmentNumber](Track, ConsignmentNumber("200000006").toOption.get)
    val reqTr6c = new Request[Track.type, ConsignmentNumber](Track, ConsignmentNumber("200000006").toOption.get)

    val reqPrNL = new Request[Pricing.type, CountryCodeIso2](Pricing, CountryCodeIso2("NL").toOption.get)
    val reqPrCN = new Request[Pricing.type, CountryCodeIso2](Pricing, CountryCodeIso2("CN").toOption.get)

    val aqs = apiQueueingService

    val call1 = aqs.executeRequests(Seq(reqSh0, reqSh1, reqSh2, reqSh3, reqSh4, reqSh5, reqSh6, reqPrNL))
    val call2 = aqs.executeRequests(Seq(reqTr0, reqTr1, reqTr2, reqTr3, reqTr4, reqTr5, reqTr6, reqPrCN))

    call1.map(apiResponseMap => {
      assert(apiResponseMap.get(reqSh0).contains(\/-(Seq(Box, Box, Pallet))))
      assert(apiResponseMap.get(reqSh1).contains(\/-(Seq(Envelope))))
      assert(apiResponseMap.get(reqSh2).contains(\/-(Seq(Box))))
      assert(apiResponseMap.get(reqSh3) == apiResponseMap.get(reqSh3)
        && apiResponseMap.get(reqSh3).contains(\/-(Seq(Pallet))))
      assert(apiResponseMap.get(reqSh4).contains(\/-(Seq(Pallet, Box))))
      assert(apiResponseMap.get(reqSh5).contains(-\/(NonSuccessResponse(503, "error"))))
      assert(apiResponseMap.get(reqSh6).contains(-\/(NonSuccessResponse(503, "error"))))

      assert(apiResponseMap.get(reqPrNL).contains(\/-(14.242090605778)))
    })

    call2.map(apiResponseMap => {
      assert(apiResponseMap.get(reqTr0).contains(\/-(New)))
      assert(apiResponseMap.get(reqTr1).contains(\/-(Collecting)))
      assert(apiResponseMap.get(reqTr2).contains(\/-(InTransit)))
      assert(apiResponseMap.get(reqTr3).contains(\/-(Collected)))
      assert(apiResponseMap.get(reqTr4).contains(\/-(Delivering)))
      assert(apiResponseMap.get(reqTr5).contains(-\/(NonSuccessResponse(503, "error"))))
      assert(apiResponseMap.get(reqTr6) == apiResponseMap.get(reqTr6)
        && apiResponseMap.get(reqTr6).contains(-\/(NonSuccessResponse(503, "error"))))

      assert(apiResponseMap.get(reqPrCN).contains(\/-(20.503467806384)))

      assert(apiResponseMap.get(reqPrNL).isEmpty)

      assert(apiResponseMap.get(reqSh0).isEmpty)
    })
  }

  it should "test invalid OrderNumber, ConsignmentNumber, CountryCodeIso2" in {
    val onWrong = OrderNumber("123")
    val cnWrong = ConsignmentNumber("321")
    val ccWrong = CountryCodeIso2("a")

    val validation = (onWrong |@| cnWrong |@| ccWrong) { (on, cn, cc) => }
    val expectedErrors = Failure[List[QueryValidationError]](List(
      InvalidOrderNumber("123"),
      InvalidConsignmentNumber("321"),
      InvalidCountryCodeIso2("a")))

    assert(validation == expectedErrors)
  }

  it should "test valid OrderNumber, ConsignmentNumber, CountryCodeIso2" in {
    val onRight = OrderNumber("123456789")
    val cnRight = ConsignmentNumber("987654321")
    val ccRight = CountryCodeIso2("AA")

    val validation = (onRight |@| cnRight |@| ccRight) { (on, cn, cc) => (on, cn, cc) }
    val expectedResult = Success((
      OrderNumber("123456789").toOption.get,
      ConsignmentNumber("987654321").toOption.get,
      CountryCodeIso2("AA").toOption.get))

    assert(validation == expectedResult)
  }

  override def afterAll {
    import scala.concurrent.duration._

    Await.ready(system.terminate(), 10 seconds)
  }

  def success(body: String): HttpResponse =
    HttpResponse(
      status = StatusCodes.OK,
      entity = HttpEntity(body)
    )

  val failure: HttpResponse =
    HttpResponse(
      status = StatusCodes.ServiceUnavailable,
      entity = HttpEntity("error")
    )

  val reqToRes: PartialFunction[HttpRequest, Future[HttpResponse]] = {
    case HttpRequestUri("shipments?q=100000000,100000001,100000002,100000003,100000004") =>
      Future.successful(success(
        """{"100000000": ["box", "box", "pallet"],
          |"100000001": ["envelope"],
          |"100000002": ["box"],
          |"100000003": ["pallet"],
          |"100000004": ["pallet", "box"]}""".stripMargin))

    case HttpRequestUri("shipments?q=100000005,100000006") =>
      Future.successful(failure)

    case HttpRequestUri("track?q=200000000,200000001,200000002,200000003,200000004") =>
      Future.successful(success(
        """{"200000000": "NEW",
          |"200000001": "COLLECTING",
          |"200000002": "IN TRANSIT",
          |"200000003": "COLLECTED",
          |"200000004": "DELIVERING"}""".stripMargin))

    case HttpRequestUri("track?q=200000005,200000006") =>
      Future.successful(failure)

    case HttpRequestUri("pricing?q=NL,CN") =>
      Future.successful(success(
        """{"NL": 14.242090605778,
          |"CN": 20.503467806384}""".stripMargin))
  }

  def apiQueueingService(implicit system: ActorSystem): ApiQueueingService = {
    new ApiQueueingService(ShipmentsUrl, TrackUrl, PricingUrl) {
      val api: ApiClient = new ApiClient {
        val requestToResponse: (HttpRequest) => Future[HttpResponse] = reqToRes
      }
    }
  }
}