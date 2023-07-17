package aqs

class QueryDep[I <: Identifier, Q <: Query]
object QueryDep {
  implicit val shipmentsQueryDep = new QueryDep[Shipments.type, OrderNumber]
  implicit val trackQueryDep = new QueryDep[Track.type, ConsignmentNumber]
  implicit val pricingQueryDep = new QueryDep[Pricing.type, CountryCodeIso2]
}