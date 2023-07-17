package aqs

import scalaz.\/

case class NonSuccessResponse(code: Int, body: String)

private[aqs] class ApiResponseMap(underlying: Map[Request[Identifier, Query], \/[NonSuccessResponse, Any]]) {
  def get[I <: Identifier, Q <: Query, R](req: Request[I, Q])
                                         (implicit queryDep: QueryDep[I, Q], responseDep: ResponseDep[I, R]) : Option[\/[NonSuccessResponse, R]] =

    underlying.get(req).asInstanceOf[Option[\/[NonSuccessResponse, R]]]
}