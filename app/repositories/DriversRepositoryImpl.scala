package repositories

import com.google.inject.{Inject, Singleton}
import database.AppDatabase
import errors.AmbigousResult
import models.{Driver, Vehicle}

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class DriversRepositoryImpl @Inject()(val database: AppDatabase, implicit val executionContext: ExecutionContext) extends DriversRepository {
  val profile = database.profile
  /**
   * Specific database profile
   */
  private val db = database.db
  private val vehicleTable = TableQuery[Vehicle.Table]
  private val driversTable = TableQuery[Driver.Table]

  override def create(driver: Driver): Future[Driver] = db.run {
    Actions.addDriver(driver)
  }

  override def find(id: String): Future[Option[Driver]] = db.run {
    Actions.findDriver(id)
  }

  override def getAll: Future[List[Driver]] = db.run {
    Actions.getAllDrivers
  }

  override def update(driver: Driver): Future[Driver] = db.run {
    Actions.updateDriver(driver)
  }

  import profile.api._

  override def delete(idx: String): Future[Boolean] = db.run {
    Actions.deleteDriver(idx)
  }

  override def assignVehicle(driverId: String, vehicle: Vehicle): Unit = db.run {
    Actions.insertAndLinkVehicle(driverId, vehicle)
  }

  object Actions {

    def findVehicle(id: String): DBIO[Option[Vehicle]] = vehicleTable.filter(_.id === id)
        .result.headOption

    def getAllDrivers: DBIO[List[Driver]] = for {
      drivers <- driversTable.result
    } yield drivers.toList

    def updateDriver(driver: Driver) = for {
      isDeleted <- deleteDriver(driver.id)
      newD <- addDriver(driver) if isDeleted
    } yield newD

    def deleteDriver(id: String): DBIO[Boolean] = for {
      maybeDelete <- driversTable.filter(_.id === id).delete
      isDeleted = if (maybeDelete == 1) true else false
    } yield isDeleted

    def addDriver(driver: Driver): DBIO[Driver] = for {
      idx <- driversTable returning driversTable.map(_.id) += driver
      maybeDriver <- findDriver(idx)
      insertedDriver <- maybeDriver match {
        case Some(value) => DBIO.successful(value)
        case _ => DBIO.failed(AmbigousResult(s"Unable to save driver [driver=$driver]"))
      }
    } yield insertedDriver

    def insertAndLinkVehicle(driverId: String, vehicle: Vehicle) = for {
      _ <- vehicleTable returning vehicleTable += vehicle
      maybeDriver <- findDriver(driverId)
      driver <- maybeDriver match {
        case Some(value) => DBIO.successful(value)
        case _ => DBIO.failed(AmbigousResult("No such driver"))
      }
      newDriver = Driver(driver.id, driver.firstName, driver.lastName, driver.email, Option(vehicle.id))
      _ <- driversTable.insertOrUpdate(newDriver)

    } yield ()

    def findDriver(id: String): DBIO[Option[Driver]] = driversTable.filter(_.id === id)
        .result.headOption

  }
}
