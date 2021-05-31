package models

import slick.jdbc.MySQLProfile.api.{Table => SlickTable, _}
import slick.lifted.{Tag => SlickTag}


case class Location(
                       address: String,
                       latitude: Double,
                       longitude: Double
                   )

object Location extends ((String, Double, Double) => Location) {
    class Table(tag: SlickTag) extends SlickTable[Location](tag, "Locations") {
        def * = (address, latitude, longitude) <> (Location.tupled, Location.unapply)

        def address = column[String]("address", O.PrimaryKey)

        def latitude = column[Double]("latitude")

        def longitude = column[Double]("longitude")
    }
}

