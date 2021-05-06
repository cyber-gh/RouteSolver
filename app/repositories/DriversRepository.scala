package repositories

import models.{Driver, Vehicle}

import scala.concurrent.Future

trait DriversRepository {
  def create(driver: Driver): Future[Driver]

  def find(id: String): Future[Option[Driver]]

  def getAll: Future[List[Driver]]

  def update(driver: Driver): Future[Driver]

  def delete(idx: String): Future[Unit]

  def assignVehicle(driverId: String, vehicle: Vehicle)
}
