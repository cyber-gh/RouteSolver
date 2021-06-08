package repositories.solver.distance

import models.Location

import scala.concurrent.Future

trait DistanceRepository {
    protected val averageSpeed: Double = 70.0

    def getDistanceMatrix(locations: List[Location]): Future[TimeDistanceResponse]
}
