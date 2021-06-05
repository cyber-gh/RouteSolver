package repositories.solver.distance

import models.Location

import scala.concurrent.Future

trait DistanceRepository {
    def getDistanceMatrix(locations: List[Location]): Future[Array[Array[Double]]]
}
