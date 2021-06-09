package repositories.drivers

import models.{Driver, Vehicle}

import scala.concurrent.Future

trait DriversRepository {
    def create(driver: Driver): Future[Driver]

    def find(id: String): Future[Option[Driver]]

    def getAll: Future[List[Driver]]

    def getAllBySupplier(supplierId: String): Future[List[Driver]]

    def delete(idx: String): Future[Boolean]

    def assignVehicle(driverId: String, vehicle: Vehicle)

    def updateLocation(driverId: String, lat: Double, lng: Double): Future[Boolean]
}
