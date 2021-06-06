package repositories.directions

import models.{Location, RouteDirections}

import scala.concurrent.Future

trait DirectionsRepository {

    def calculateDirections(locations: List[Location]): Future[RouteDirections]
}
