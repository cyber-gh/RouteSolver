package models

import akka.http.scaladsl.model.DateTime
import models.RouteState.RouteState
import repositories.solver.utility.DeliveryOrder
import slick.jdbc.MySQLProfile.api.{Table => SlickTable, _}
import slick.lifted.{Tag => SlickTag}

import java.sql.Timestamp


case class DeliveryOrderModel(
                                 id: String,
                                 name: String,
                                 routeId: String,
                                 locationId: String,

                                 startTime: Option[String],
                                 endTime: Option[String],

                                 weight: Option[Double],
                                 volume: Option[Double]

                             ) extends Identifiable {
    def toFullOrder(location: Location): DeliveryOrder = {
        DeliveryOrder(id, routeId, location, startTime, endTime, weight, volume)
    }
}

object RouteState extends Enumeration {
    type RouteState = Value
    val Delivered = Value("delivered")
    val InDelivery = Value("in_delivery")
    val Optimized = Value("optimized")
    val Idle = Value("idle")
}

case class DeliveryRouteModel(
                                 id: String,
                                 supplierId: String,
                                 name: String,
                                 startLocationId: String,
                                 startTime: DateTime,
                                 state: RouteState,

                                 selectedSolutionId: Option[String],

                                 roundTrip: Boolean
                             ) extends Identifiable


object DeliveryRouteModel extends ((String, String, String, String, DateTime, RouteState, Option[String], Boolean) => DeliveryRouteModel) {

    private implicit val dateTimeColumnType = MappedColumnType.base[DateTime, Timestamp](
        dt => new Timestamp(dt.clicks),
        ts => DateTime(ts.getTime)
    )

    private implicit val routeStateColumnType = MappedColumnType.base[RouteState, String](
        state => state.toString,
        raw => raw match {
            case "in_delivery" => RouteState.InDelivery
            case "delivered" => RouteState.Delivered
            case "optimized" => RouteState.Optimized
            //            case RouteState.Idle.toString => RouteState.Idle
            case _ => RouteState.Idle
        }
    )

    class Table(tag: SlickTag) extends SlickTable[DeliveryRouteModel](tag, "Routes") {
        lazy val locations = TableQuery[Location.Table]
        lazy val suppliers = TableQuery[Supplier.Table]

        def * = (id, supplierId, name, startLocationId, startTime, state, selectedSolutionId, roundTrip) <> (DeliveryRouteModel.tupled, DeliveryRouteModel.unapply)

        def id = column[String]("id", O.PrimaryKey)

        def supplierId = column[String]("supplier_id")

        def name = column[String]("name")

        def startLocationId = column[String]("start_location_id")

        def startTime = column[DateTime]("start_time")

        def state = column[RouteState]("state")

        def roundTrip = column[Boolean]("round_trip")

        def selectedSolutionId = column[Option[String]]("selected_solution_id")

        def supplier = foreignKey("supplier", supplierId, suppliers)(_.id)

        def location = foreignKey("address", startLocationId, locations)(_.address)
    }
}


object DeliveryOrderModel extends ((String, String, String, String, Option[String], Option[String], Option[Double], Option[Double]) => DeliveryOrderModel) {
    class Table(tag: SlickTag) extends SlickTable[DeliveryOrderModel](tag, "Orders") {
        lazy val routes = TableQuery[DeliveryRouteModel.Table]
        lazy val locations = TableQuery[Location.Table]

        def * = (id, name, routeId, locationId, startTime, endTime, weight, volume) <> (DeliveryOrderModel.tupled, DeliveryOrderModel.unapply)

        def id = column[String]("id", O.PrimaryKey)

        def name = column[String]("name")

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


