package models


import slick.jdbc.MySQLProfile.api.{Table => SlickTable, _}
import slick.lifted.{Tag => SlickTag}


case class Vehicle(
                      id: String,
                      licensePlate: String,
                      fuelConsumption: Double,
                      volumeCapacity: Double,
                      weightCapacity: Double
                  ) extends Identifiable


case class Supplier(
                       id: String,
                       name: String,
                       email: String
                   ) extends AppUser

case class Driver(
                     id: String,
                     name: String,
                     email: String,
                     locationId: String,

                     vehicleId: Option[String],
                     supplierId: Option[String]
                 ) extends AppUser


object Supplier extends ((String, String, String) => Supplier) {
    class Table(tag: SlickTag) extends SlickTable[Supplier](tag, "Suppliers") {
        def * = (id, name, email) <> (Supplier.tupled, Supplier.unapply)

        def id = column[String]("id", O.PrimaryKey)

        def name = column[String]("name")

        def email = column[String]("email")
    }
}

object Vehicle extends ((String, String, Double, Double, Double) => Vehicle) {
    class Table(tag: SlickTag) extends SlickTable[Vehicle](tag, "Vehicles") {
        def * = (id, licensePlate, fuelConsumption, volumeCapacity, weightCapacity) <> (Vehicle.tupled, Vehicle.unapply)

        def licensePlate = column[String]("license_plate")

        def fuelConsumption = column[Double]("fuel_consumption")

        def volumeCapacity = column[Double]("volume_capacity")

        def weightCapacity = column[Double]("weight_capacity")

        def id = column[String]("id", O.PrimaryKey)
    }
}


object Driver extends ((String, String, String, String, Option[String], Option[String]) => Driver) {
    class Table(tag: SlickTag) extends SlickTable[Driver](tag, "Drivers") {
        private lazy val vehicles = TableQuery[Vehicle.Table]
        private lazy val suppliers = TableQuery[Supplier.Table]

        def * = (id, name, email, locationId, vehicleId, supplierId) <> (Driver.tupled, Driver.unapply)

        def id = column[String]("id", O.PrimaryKey)

        def email = column[String]("email")

        def vehicleId = column[Option[String]]("vehicle_id")

        def locationId = column[String]("location_id")

        def name = column[String]("name")

        def supplierId = column[Option[String]]("supplier_id")

        def vehicle = foreignKey("vehicle", vehicleId, vehicles)(_.id)

        def supplier = foreignKey("supplier", supplierId, suppliers)(_.id)
    }
}