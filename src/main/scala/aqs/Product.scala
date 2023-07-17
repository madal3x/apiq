package aqs

sealed trait Product
case object Envelope extends Product
case object Box extends Product
case object Pallet extends Product