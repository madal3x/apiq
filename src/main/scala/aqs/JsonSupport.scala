package aqs

import spray.json._

object JsonSupport {
  implicit val orderNumberJson = new JsonFormat[OrderNumber] {
    def read(value: JsValue): OrderNumber = value match {
      case JsString(number) => new OrderNumber(number)
      case _ => deserializationError("String expected")
    }

    def write(o: OrderNumber): JsValue = serializationError("not supported")
  }

  implicit val consignmentNumberJson = new JsonFormat[ConsignmentNumber] {
    def read(value: JsValue): ConsignmentNumber = value match {
      case JsString(number) => new ConsignmentNumber(number)
      case _ => deserializationError("String expected")
    }

    def write(o: ConsignmentNumber): JsValue = serializationError("not supported")
  }

  implicit val countryCodeIso2Json = new JsonFormat[CountryCodeIso2] {
    def read(value: JsValue): CountryCodeIso2 = value match {
      case JsString(iso2) => new CountryCodeIso2(iso2)
      case _ => deserializationError("String expected")
    }

    def write(o: CountryCodeIso2): JsValue = serializationError("not supported")
  }

  implicit val productJson = new JsonFormat[Product] {
    def read(value: JsValue): Product = value match {
      case JsString("envelope") => Envelope
      case JsString("box") => Box
      case JsString("pallet") => Pallet
      case _ => deserializationError("Product expected")
    }

    def write(p: Product): JsValue = serializationError("not supported")
  }

  implicit val trackingStatusJson = new JsonFormat[TrackingStatus] {
    def read(value: JsValue): TrackingStatus = value match {
      case JsString("NEW") => New
      case JsString("IN TRANSIT") => InTransit
      case JsString("COLLECTING") => Collecting
      case JsString("COLLECTED") => Collected
      case JsString("DELIVERING") => Delivering
      case JsString("DELIVERED") => Delivered
      case _ => deserializationError("TrackingStatus expected")
    }

    def write(s: TrackingStatus): JsValue = serializationError("not supported")
  }
}