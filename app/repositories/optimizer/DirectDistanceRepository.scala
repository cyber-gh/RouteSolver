package repositories.optimizer

import models.Location

import scala.concurrent.Future

class DirectDistanceRepository extends DistanceRepository {

    override def getDistanceMatrix(locations: List[Location]): Future[Array[Array[Double]]] = Future.successful(
        locations.map(x => {
            locations.map(y => {
                distanceBetween(x, y)
            }).toArray
        }).toArray
    )

    private def distanceBetween(a: Location, b: Location): Double = {
        val R = 6371e3
        val pi = Math.PI
        val fi_1: Double = a.latitude * pi / 180
        val fi_2 = b.latitude * pi / 180
        val delta_fi = (b.latitude - a.latitude) * pi / 180
        val delta_lambda = (b.longitude - a.longitude) * pi / 180

        val x = Math.sin(delta_fi / 2) * Math.sin(delta_fi / 2) +
            Math.cos(fi_1) * Math.cos(fi_2) *
                Math.sin(delta_lambda / 2) * Math.sin(delta_lambda / 2);
        val c = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x))
        val d = R * c

        d
    }
}
