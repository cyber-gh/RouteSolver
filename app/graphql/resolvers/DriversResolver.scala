package graphql.resolvers

import com.google.inject.Inject
import models.Driver
import repositories.drivers.DriversRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DriversResolver @Inject() (driversRepository: DriversRepository, implicit val executionContext: ExecutionContext) {

    def drivers: Future[List[Driver]] = driversRepository.getAll

    def addDriver(name: String, email: String): Future[Driver] = driversRepository.create(
        Driver(UUID.randomUUID().toString, name = name, email = email, null, null)
    )

    def findDriver(idx: String): Future[Option[Driver]] = driversRepository.find(idx)

    def deleteDriver(idx: String): Future[Boolean] = driversRepository.delete(idx)
}
