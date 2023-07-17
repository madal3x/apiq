package aqs

sealed trait TrackingStatus
case object New extends TrackingStatus
case object InTransit extends TrackingStatus
case object Collecting extends TrackingStatus
case object Collected extends TrackingStatus
case object Delivering extends TrackingStatus
case object Delivered extends TrackingStatus