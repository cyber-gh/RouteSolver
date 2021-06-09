package repositories.drivers

import com.google.inject.{Inject, Singleton}
import database.AppDatabase
import errors.{AmbigousResult, EntityNotFound}
import models.{Driver, Vehicle}
import repositories.auth0.Auth0Management
import repositories.locations.LocationRepository
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class DriversRepositoryImpl @Inject()(val database: AppDatabase, val auth0Api: Auth0Management, val locationRepository: LocationRepository, implicit val executionContext: ExecutionContext) extends DriversRepository {

    override def updateLocation(driverId: String, lat: Double, lng: Double): Future[Boolean] = for {
        maybeDriver <- find(driverId)
        driver <- maybeDriver match {
            case Some(value) => Future.successful(value)
            case None => Future.failed(EntityNotFound(s"No such driver ${driverId}"))
        }
        location <- locationRepository.getLocation(lat, lng)
        newDriver = driver.copy(locationId = location.address)
        _ <- db.run {
            Actions.addDriver(newDriver)
        }
    } yield true

    override def create(driver: Driver): Future[Driver] = for {
        user <- auth0Api.registerDriver(driver.name, driver.email)
        newLocationId <- locationRepository.getLocation(driver.locationId).map(x => x.address)

        driver <- db.run {
            Actions.addDriver(driver.copy(id = user.getId, locationId = newLocationId))
        }
    } yield driver

    override def find(id: String): Future[Option[Driver]] = db.run {
        Actions.findDriver(id)
    }

    val profile = database.profile

    override def getAll: Future[List[Driver]] = for {
        drivers <- db.run {
            Actions.getAllDrivers
        }
    } yield drivers

    /**
     * Specific database profile
     */
    private val db = database.db
    private val vehicleTable = TableQuery[Vehicle.Table]
    private val driversTable = TableQuery[Driver.Table]

    override def getAllBySupplier(supplierId: String): Future[List[Driver]] = for {
        //        users <- auth0Api.listDrivers
        drivers <- db.run {
            Actions.getDriversBySupplier(supplierId)
        }
        //        ans = combineDetails(users, drivers)
    } yield drivers
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

        def getDriversBySupplier(suppliedId: String): DBIO[List[Driver]] = for {
            drivers <- driversTable.filter(_.supplierId === suppliedId).result
        } yield drivers.toList

        def getAllDrivers: DBIO[List[Driver]] = for {
            drivers <- driversTable.result
        } yield drivers.toList

        def deleteDriver(id: String): DBIO[Boolean] = for {
            maybeDelete <- driversTable.filter(_.id === id).delete
            isDeleted = if (maybeDelete == 1) true else false
        } yield isDeleted

        def addDriver(driver: Driver): DBIO[Driver] = for {
            insertedDriver <- driversTable.insertOrUpdate(driver).map(_ => driver)
        } yield insertedDriver

        def insertAndLinkVehicle(driverId: String, vehicle: Vehicle) = for {
            _ <- vehicleTable.insertOrUpdate(vehicle)
            maybeDriver <- findDriver(driverId)
            driver <- maybeDriver match {
                case Some(value) => DBIO.successful(value)
                case _ => DBIO.failed(AmbigousResult("No such driver"))
            }
            newDriver = Driver(driver.id, driver.name, driver.email, driver.locationId, Option(vehicle.id), driver.supplierId)
            _ <- driversTable.insertOrUpdate(newDriver)

        } yield ()

        def findDriver(id: String): DBIO[Option[Driver]] = driversTable.filter(_.id === id)
            .result.headOption

    }
}
