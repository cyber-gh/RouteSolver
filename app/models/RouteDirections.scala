package models

import slick.jdbc.MySQLProfile.api.{Table => SlickTable, _}
import slick.lifted.{Tag => SlickTag}

case class RouteDirections(
                              id: String,

                              geometry: String,

                              distance: Double,
                              duration: Double

                          ) extends Identifiable

object RouteDirections extends ((String, String, Double, Double) => RouteDirections) {
    class Table(tag: SlickTag) extends SlickTable[RouteDirections](tag, "RouteDirections") {
        def * = (id, geometry, distance, duration) <> (RouteDirections.tupled, RouteDirections.unapply)

        def id = column[String]("id", O.PrimaryKey)

        def geometry = column[String]("geometry")

        def distance = column[Double]("distance")

        def duration = column[Double]("duration")
    }
}
