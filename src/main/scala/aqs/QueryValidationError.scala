package aqs

sealed trait QueryValidationError

case class InvalidOrderNumber(number: String) extends QueryValidationError {
  override def toString = s"Order number [$number] should have 9 digits."
}

case class InvalidConsignmentNumber(number: String) extends QueryValidationError {
  override def toString = s"Consignment number [$number] should have 9 digits."
}

case class InvalidCountryCodeIso2(iso2: String) extends QueryValidationError {
  override def toString = s"Country code iso2 [$iso2] should have 2 capital case letters."
}