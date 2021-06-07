package models

import slick.jdbc.MySQLProfile.api.{Table => SlickTable, _}
import slick.lifted.{Tag => SlickTag}

case class DeliveryClientInputForm(
                                      name: String,
                                      email: String,
                                      address: String,

                                      startTime: Option[String],
                                      endTime: Option[String],

                                      weight: Option[Double],
                                      volume: Option[Double]
                                  )

case class DeliveryClient(
                             id: String,
                             name: String,
                             email: String,
                             locationId: String,
                             supplierId: Option[String],

                             startTime: Option[String],
                             endTime: Option[String],

                             weight: Option[Double],
                             volume: Option[Double]
                         ) extends AppUser

object DeliveryClient extends ((String, String, String, String, Option[String], Option[String], Option[String], Option[Double], Option[Double]) => DeliveryClient) {
    class Table(tag: SlickTag) extends SlickTable[DeliveryClient](tag, "Clients") {
        private val locations = TableQuery[Location.Table]

        def * = (id, email, name, locationId, supplierId, startTime, endTime, weight, volume) <> (DeliveryClient.tupled, DeliveryClient.unapply)

        def id = column[String]("id", O.PrimaryKey)

        def email = column[String]("email")

        def name = column[String]("name")

        def locationId = column[String]("location_id")

        def supplierId = column[Option[String]]("supplier_id")

        def startTime = column[Option[String]]("start_time")

        def endTime = column[Option[String]]("end_time")

        def weight = column[Option[Double]]("weight")

        def volume = column[Option[Double]]("volume")


        def location = foreignKey("location", locationId, locations)(_.address)
    }
}