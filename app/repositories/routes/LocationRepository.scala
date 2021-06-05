package repositories.routes

import models.Location

import scala.concurrent.Future

trait LocationRepository {
    def getLocation(address: String): Future[Location]

    def getLocations(addresses: List[String]): Future[List[Location]]

    def getLocation(lat: Double, lng: Double): Future[Location]
}
