package repositories.solver.optimizer

import models.Location
import repositories.solver.utility.{DeliveryOrder, OptimizeSolution}

import scala.concurrent.Future

trait RouteOptimizer {
    def optimize(start: Location, orders: List[DeliveryOrder]): Future[OptimizeSolution]
}


