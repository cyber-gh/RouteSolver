package graphql.resolvers

import com.google.inject.Inject
import models.Driver
import repositories.DriversRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DriversResolver @Inject() (driversRepository: DriversRepository, implicit val executionContext: ExecutionContext) {

    def drivers: Future[List[Driver]] = driversRepository.getAll

    def addDriver(firstName: String, lastName: String, email: String): Future[Driver] = driversRepository.create(
        Driver(UUID.randomUUID().toString, firstName = firstName, lastName = lastName, email = email, null)
    )

    def findDriver(idx: String): Future[Option[Driver]] = driversRepository.find(idx)

    def updateDriver(idx: String, firstName: String, lastName: String, email: String): Future[Driver] = driversRepository.update(
        Driver(idx, firstName = firstName, lastName = lastName, email = email, null)
    )

    def deleteDriver(idx: String): Future[Boolean] = driversRepository.delete(idx)
}
