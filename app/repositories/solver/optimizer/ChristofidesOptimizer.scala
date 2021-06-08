package repositories.solver.optimizer

import com.google.inject.Inject
import errors.OperationNotPermitted
import models.Location
import repositories.solver.distance.DistanceRepository
import repositories.solver.optimizer.alg.ChristofidesAlg
import repositories.solver.utility.{DeliveryOrder, OptimizeSolution}

import scala.concurrent.{ExecutionContext, Future}


class ChristofidesOptimizer @Inject()(distanceRepository: DistanceRepository, implicit val executionContext: ExecutionContext) extends RouteOptimizer {
    override def optimize(start: Location, orders: List[DeliveryOrder]): Future[OptimizeSolution] = {
        val locations = List(start) ++ orders.map(_.location)
        if (locations.length > 10) return Future.failed(OperationNotPermitted("Cannot apply backtarck for more than 10 orders"))
        return for {
            distances <- distanceRepository.getDistanceMatrix(locations).map(x => x.distances)
            optimizer = new ChristofidesAlg(distances, locations.length, false, false)
            (ans, cost) = optimizer.solve()
            finalOrders = ans.drop(1).zipWithIndex.map { case (o, idx) => (orders(o - 1), idx) }
            sortedOrders = finalOrders.sortBy { case (order, idx) => idx }
            sol = OptimizeSolution(sortedOrders, cost)
        } yield sol
    }
}

