package graphql.resolvers

import com.google.inject.Inject
import models.Driver
import repositories.drivers.DriversRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DriversResolver @Inject() (driversRepository: DriversRepository, implicit val executionContext: ExecutionContext) {

    def drivers(supplierId: String): Future[List[Driver]] = driversRepository.getAllBySupplier(supplierId)

    def addDriver(name: String, email: String, address: String, supplierId: String): Future[Driver] = driversRepository.create(
        Driver(UUID.randomUUID().toString, name = name, email = email, locationId = address, vehicleId = null, supplierId = Option(supplierId))
    )

    def findDriver(idx: Option[String]): Future[Option[Driver]] = idx match {
        case Some(value) => driversRepository.find(value)
        case None => Future.successful(Option.empty)
    }

    def deleteDriver(idx: String): Future[Boolean] = driversRepository.delete(idx)

    def updateLocation(driverId: String, lat: Double, lng: Double): Future[Boolean] = driversRepository.updateLocation(driverId, lat, lng)
}
