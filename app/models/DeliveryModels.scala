package models

import slick.jdbc.MySQLProfile.api.{Table => SlickTable, _}
import slick.lifted.{Tag => SlickTag}

case class Location(
                       address: String,
                       latitude: Double,
                       longitude: Double)

case class DeliveryRoute(
                            id: String,

                            startLocation: Location,
                            orders: List[DeliveryOrder],

                            roundTrip: Boolean
                        ) extends Identifiable


case class DeliveryOrder(
                            id: String,

                            location: Location,

                            startTime: Option[String],
                            endTime: Option[String],

                            weight: Option[Double],
                            volume: Option[Double]

                        ) extends Identifiable

case class DeliveryOrderModel(
                                 id: String,

                                 routeId: String,
                                 locationId: String,

                                 startTime: Option[String],
                                 endTime: Option[String],

                                 weight: Option[Double],
                                 volume: Option[Double]

                             ) extends Identifiable

case class DeliveryRouteModel(
                                 id: String,
                                 supplierId: String,
                                 startLocationId: String,

                                 roundTrip: Boolean
                             ) extends Identifiable

object Location extends ((String, Double, Double) => Location) {
    class Table(tag: SlickTag) extends SlickTable[Location](tag, "Locations") {
        def * = (address, latitude, longitude) <> (Location.tupled, Location.unapply)

        def address = column[String]("address", O.PrimaryKey)

        def latitude = column[Double]("latitude")

        def longitude = column[Double]("longitude")
    }
}

object DeliveryRouteModel extends ((String, String, String, Boolean) => DeliveryRouteModel) {
    class Table(tag: SlickTag) extends SlickTable[DeliveryRouteModel](tag, "Routes") {
        lazy val locations = TableQuery[Location.Table]
        lazy val suppliers = TableQuery[Supplier.Table]

        def * = (id, supplierId, startLocationId, roundTrip) <> (DeliveryRouteModel.tupled, DeliveryRouteModel.unapply)

        def id = column[String]("id", O.PrimaryKey)

        def supplierId = column[String]("supplier_id")

        def startLocationId = column[String]("start_location_id")

        def roundTrip = column[Boolean]("round_trip")

        def supplier = foreignKey("supplier", supplierId, suppliers)(_.id)

        def location = foreignKey("address", startLocationId, locations)(_.address)
    }
}


object DeliveryOrderModel extends ((String, String, String, Option[String], Option[String], Option[Double], Option[Double]) => DeliveryOrderModel) {
    class Table(tag: SlickTag) extends SlickTable[DeliveryOrderModel](tag, "Orders") {
        lazy val routes = TableQuery[DeliveryRouteModel.Table]
        lazy val locations = TableQuery[Location.Table]

        def * = (id, routeId, locationId, startTime, endTime, weight, volume) <> (DeliveryOrderModel.tupled, DeliveryOrderModel.unapply)

        def id = column[String]("id", O.PrimaryKey)

        def locationId = column[String]("location_id")

        def startTime = column[Option[String]]("start_time")

        def endTime = column[Option[String]]("end_time")

        def weight = column[Option[Double]]("weight")

        def volume = column[Option[Double]]("volume")

        def route = foreignKey("route", routeId, routes)(_.id)

        def routeId = column[String]("route_id")

        def location = foreignKey("location", locationId, locations)(_.address)
    }
}



