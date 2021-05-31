package models

import slick.jdbc.MySQLProfile.api.{Table => SlickTable, _}
import slick.lifted.{Tag => SlickTag}

case class DeliveryClient(
                             id: String,
                             name: String,
                             email: String,
                             locationId: String,

                             supplierId: Option[String]
                         ) extends AppUser

object DeliveryClient extends ((String, String, String, String, Option[String]) => DeliveryClient) {
    class Table(tag: SlickTag) extends SlickTable[DeliveryClient](tag, "Clients") {
        private val locations = TableQuery[Location.Table]

        def * = (id, email, name, locationId, supplierId) <> (DeliveryClient.tupled, DeliveryClient.unapply)

        def id = column[String]("id", O.PrimaryKey)

        def email = column[String]("email")

        def name = column[String]("name")

        def locationId = column[String]("location_id")

        def supplierId = column[Option[String]]("supplier_id")

        def location = foreignKey("location", locationId, locations)(_.address)
    }
}