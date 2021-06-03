package repositories.optimizer

import models.{DeliveryOrderModel, Location}
import repositories.optimizer.utility.DeliveryOrder

trait RouteOptimizer {
    def optimize(startLocation: Location, orders: List[DeliveryOrder]): List[DeliveryOrderModel]
}
