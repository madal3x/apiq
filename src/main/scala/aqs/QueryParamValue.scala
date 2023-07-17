package aqs

trait QueryParamValue[Q <: Query] {
  def queryParamValue(q: Q): String
}

object QueryParamValue {
  implicit val orderNumberQPV = new QueryParamValue[OrderNumber] {
    def queryParamValue(q: OrderNumber) = q.number
  }

  implicit val consignmentNumberQPV = new QueryParamValue[ConsignmentNumber] {
    def queryParamValue(q: ConsignmentNumber) = q.number
  }

  implicit val countryCodeIso2QPV = new QueryParamValue[CountryCodeIso2] {
    def queryParamValue(q: CountryCodeIso2) = q.iso2
  }
}