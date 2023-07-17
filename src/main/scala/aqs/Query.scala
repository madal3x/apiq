package aqs

import org.apache.commons.lang3.StringUtils

sealed trait Query

import scalaz._, Scalaz._

final class ConsignmentNumber private[aqs] (val number: String) extends Query {
  def canEqual(a: Any) = a.isInstanceOf[ConsignmentNumber]
  override def hashCode = 31 * number.hashCode
  override def equals(other: Any) = other match {
    case that: ConsignmentNumber => that.canEqual(this) && this.hashCode == that.hashCode
    case _ => false
  }
  override def toString: String = s"OrderNumber($number)"
}

object ConsignmentNumber {
  def apply(number: String): Validation[List[QueryValidationError], ConsignmentNumber] =
    if (StringUtils.isNumeric(number) && number.length == 9)
      new ConsignmentNumber(number).success
    else
      List(InvalidConsignmentNumber(number)).failure

  def unapply(arg: ConsignmentNumber): Option[String] =
    Some(arg.number)
}

final class OrderNumber private[aqs] (val number: String) extends Query {
  def canEqual(a: Any) = a.isInstanceOf[OrderNumber]
  override def hashCode = 41 * number.hashCode
  override def equals(other: Any) = other match {
    case that: OrderNumber => that.canEqual(this) && this.hashCode == that.hashCode
    case _ => false
  }
  override def toString: String = s"ConsignmentNumber($number)"
}

object OrderNumber {
  def apply(number: String): Validation[List[QueryValidationError], OrderNumber] =
    if (StringUtils.isNumeric(number) && number.length == 9)
      new OrderNumber(number).success
    else
      List(InvalidOrderNumber(number)).failure

  def unapply(arg: OrderNumber): Option[String] =
    Some(arg.number)
}

final class CountryCodeIso2 private[aqs] (val iso2: String) extends Query {
  def canEqual(a: Any) = a.isInstanceOf[CountryCodeIso2]
  override def hashCode = 51 * iso2.hashCode
  override def equals(other: Any) = other match {
    case that: CountryCodeIso2 => that.canEqual(this) && this.hashCode == that.hashCode
    case _ => false
  }
  override def toString: String = s"CountryCodeIso2($iso2)"
}

object CountryCodeIso2 {
  def apply(iso2: String): Validation[List[QueryValidationError], CountryCodeIso2] =
    if (StringUtils.isAllUpperCase(iso2) && iso2.length == 2)
      new CountryCodeIso2(iso2).success
    else
      List(InvalidCountryCodeIso2(iso2)).failure

  def unapply(arg: CountryCodeIso2): Option[String] =
    Some(arg.iso2)
}