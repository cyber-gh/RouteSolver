package models

import slick.jdbc.MySQLProfile.api.{Table => SlickTable, _}
import slick.lifted.{Tag => SlickTag}

case class DeliveryClient(
                             id: String,
                             name: String,
                             email: String,
                             address: String,
                             latitude: Option[Double],
                             longitude: Option[Double],

                             supplierId: Option[String]
                         ) extends AppUser

object DeliveryClient extends ((String, String, String, String, Option[Double], Option[Double], Option[String]) => DeliveryClient) {
    class Table(tag: SlickTag) extends SlickTable[DeliveryClient](tag, "Clients") {
        def * = (id, email, name, address, latitude, longitude, supplierId) <> (DeliveryClient.tupled, DeliveryClient.unapply)

        def id = column[String]("id", O.PrimaryKey)

        def email = column[String]("email")

        def name = column[String]("name")

        def address = column[String]("address")

        def latitude = column[Option[Double]]("latitude")

        def longitude = column[Option[Double]]("longitude")

        def supplierId = column[Option[String]]("supplier_id")
    }
}