package models

import akka.http.scaladsl.model.DateTime
import slick.jdbc.MySQLProfile.api.{Table => SlickTable, _}
import slick.lifted.{Tag => SlickTag}

import java.sql.Timestamp


case class RouteSolution(
                            id: String,
                            routeId: String,

                            algorithm: String,

                            nrOrders: Int,
                            distance: Double, // in meters
                            time: Double, // in seconds

                            totalWeight: Option[Double],
                            totalVolume: Option[Double]
                        ) extends Identifiable

case class DeliveryOrderSolution(
                                    id: String,
                                    solutionId: String,
                                    orderId: String,


                                    order: Int,
                                    estimatedArrivalTime: DateTime,
                                    estimatedDepartureTime: DateTime
                                ) extends Identifiable

object DeliveryOrderSolution extends ((String, String, String, Int, DateTime, DateTime) => DeliveryOrderSolution) {
    private implicit val dateTimeColumnType = MappedColumnType.base[DateTime, Timestamp](
        dt => new Timestamp(dt.clicks),
        ts => DateTime(ts.getTime)
    )

    class Table(tag: SlickTag) extends SlickTable[DeliveryOrderSolution](tag, "OrderSolutions") {
        def * = (id, solutionId, orderId, order, estimatedArrivalTime, estimatedDepartureTime) <> (DeliveryOrderSolution.tupled, DeliveryOrderSolution.unapply)

        def id = column[String]("id", O.PrimaryKey)

        def solutionId = column[String]("solution_id")

        def orderId = column[String]("order_id")

        def order = column[Int]("delivery_order")

        def estimatedArrivalTime = column[DateTime]("estimated_arrival_time")

        def estimatedDepartureTime = column[DateTime]("estimated_departure_time")

    }
}

object RouteSolution extends ((String, String, String, Int, Double, Double, Option[Double], Option[Double]) => RouteSolution) {
    class Table(tag: SlickTag) extends SlickTable[RouteSolution](tag, "RouteSolutions") {
        def * = (id, routeId, algorithm, nrOrders, distance, time, totalWeight, totalVolume) <> (RouteSolution.tupled, RouteSolution.unapply)

        def id = column[String]("id")

        def routeId = column[String]("route_id")

        def algorithm = column[String]("algorithm")

        def nrOrders = column[Int]("nr_orders")

        def distance = column[Double]("distance")

        def time = column[Double]("time")

        def totalWeight = column[Option[Double]]("total_weight")

        def totalVolume = column[Option[Double]]("total_volume")
    }
}