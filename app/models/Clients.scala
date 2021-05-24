package models

import slick.jdbc.MySQLProfile.api.{Table => SlickTable, _}
import slick.lifted.{Tag => SlickTag}

case class DeliveryClient(
                             id: String,
                             name: String,
                             email: String,
                             address: String,
                             latitude: Double,
                             longitude: Double
                         ) extends AppUser

object DeliveryClient extends ((String, String, String, String, Double, Double) => DeliveryClient) {
    class Table(tag: SlickTag) extends SlickTable[DeliveryClient](tag, "Clients") {
        def * = (id, email, name, address, latitude, longitude) <> (DeliveryClient.tupled, DeliveryClient.unapply)

        def id = column[String]("id", O.PrimaryKey)

        def email = column[String]("email")

        def name = column[String]("name")

        def address = column[String]("address")

        def latitude = column[Double]("latitude")

        def longitude = column[Double]("longitude")
    }
}