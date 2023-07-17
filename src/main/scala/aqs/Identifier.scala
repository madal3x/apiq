package aqs

sealed trait Identifier
case object Shipments extends Identifier
case object Track extends Identifier
case object Pricing extends Identifier