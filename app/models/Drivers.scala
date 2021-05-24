package models


import slick.jdbc.MySQLProfile.api.{Table => SlickTable, _}
import slick.lifted.{Tag => SlickTag}


case class Vehicle(
                      id: String,
                      licensePlate: String,
                      model: String,
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
                     vehicleId: Option[String],
                     supplierId: Option[String]
                 ) extends AppUser






object Vehicle extends ( (String, String, String, Double, Double, Double) => Vehicle ) {
  class Table(tag: SlickTag) extends SlickTable[Vehicle](tag, "Vehicles") {
    def id = column[String]("ID", O.PrimaryKey)
    def licensePlate = column[String]("license_plate")
    def model = column[String]("model")
    def fuelConsumption = column[Double]("fuel_consumption")
    def volumeCapacity = column[Double]("volume_capacity")
    def weightCapacity = column[Double]("weight_capacity")

    def * = (id, licensePlate, model, fuelConsumption, volumeCapacity, weightCapacity) <> (Vehicle.tupled, Vehicle.unapply)
  }
}


object Driver extends ((String, String, String, Option[String], Option[String]) => Driver) {
    class Table(tag: SlickTag) extends SlickTable[Driver](tag, "Drivers") {
        def * = (id, name, email, vehicleId, supplierId) <> (Driver.tupled, Driver.unapply)

        def id = column[String]("ID", O.PrimaryKey)

        def email = column[String]("email")

        def vehicleId = column[Option[String]]("vehicle_id")

        def name = column[String]("name")

        def supplierId = column[Option[String]]("supplier_id")
    }
}