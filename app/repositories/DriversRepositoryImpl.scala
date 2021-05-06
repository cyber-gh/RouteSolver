package repositories

import com.google.inject.{Inject, Singleton}
import database.AppDatabase
import models.{Driver, Vehicle}

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class DriversRepositoryImpl @Inject() (val db: AppDatabase, implicit val executionContext: ExecutionContext) extends DriversRepository {
  override def create(driver: Driver): Future[Driver] = ???

  override def find(id: String): Future[Option[Driver]] = ???

  override def getAll: Future[List[Driver]] = ???

  override def update(driver: Driver): Future[Driver] = ???

  override def delete(idx: String): Future[Unit] = ???

  override def assignVehicle(driverId: String, vehicle: Vehicle): Unit = ???
}
