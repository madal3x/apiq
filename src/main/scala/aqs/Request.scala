package aqs

class Request[+I <: Identifier, +Q <: Query](val identifier: I, val query: Q)
                                            (implicit queryDep: QueryDep[I, Q]) {

  override def toString: String = s"$identifier $query"
}

object Request {
  def unapply[I <: Identifier, Q <: Query](arg: Request[I, Q]): Option[(Identifier, Q)] =
    Some((arg.identifier, arg.query))
}