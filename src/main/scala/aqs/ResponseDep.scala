package aqs

class ResponseDep[I <: Identifier, R]
object ResponseDep {
  implicit val shipmentsResponseDep = new ResponseDep[Shipments.type, Seq[Product]]
  implicit val trackResponseDep = new ResponseDep[Track.type, TrackingStatus]
  implicit val pricingResponseDep = new ResponseDep[Pricing.type, Double]
}