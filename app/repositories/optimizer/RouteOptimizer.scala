package repositories.optimizer

import models.Location
import repositories.optimizer.utility.DeliveryOrder

import scala.concurrent.Future

trait RouteOptimizer {
    def optimize(start: Location, orders: List[DeliveryOrder]): Future[List[(DeliveryOrder, Int)]]
}
