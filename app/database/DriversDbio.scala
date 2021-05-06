package database

import com.google.inject.Inject
import models.{Driver, Vehicle}

import scala.concurrent.ExecutionContext



class DriversDbio @Inject() (val database: AppDatabase, implicit val executionContext: ExecutionContext) {

  val db = database.db

  /**
   * Specific database profile
   */
  val profile = database.profile

  import profile.api._

  val vehicleTable = TableQuery[Vehicle.Table]

  def find(id: String): DBIO[Option[Vehicle]] = vehicleTable.filter(_.fuelConsumption > 0.2)
    .result.headOption

}
