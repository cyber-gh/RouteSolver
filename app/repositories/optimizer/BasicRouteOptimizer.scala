package repositories.optimizer

import com.google.inject.Inject
import models.Location
import repositories.optimizer.utility.DeliveryOrder

import scala.concurrent.{ExecutionContext, Future}


class BacktrackOptimizer(val distanceMatrix: Array[Array[Double]], val nr: Int) {

    def run(): (List[Int], Double) = {
        val sol = Range(0, nr).toList.permutations
            .filter(l => l.head == 0)
            .minBy(x => cost(x))
        return (sol, cost(sol))
    }

    private def cost(p: List[Int]): Double = {
        val pairs = p.sliding(2).collect { case List(a, b) => (a, b) }.toList
        return pairs.map { case (x, y) => distanceMatrix(x)(y) }.sum
    }
}

class BasicRouteOptimizer @Inject()(distanceRepository: DistanceRepository, implicit val executionContext: ExecutionContext) extends RouteOptimizer {
    override def optimize(start: Location, orders: List[DeliveryOrder]): Future[List[(DeliveryOrder, Int)]] = {
        val locations = List(start) ++ orders.map(_.location)
        return for {
            distanceMatrix <- distanceRepository.getDistanceMatrix(locations)
            optimizer = new BacktrackOptimizer(distanceMatrix, locations.length)
            (ans, cost) = optimizer.run()
            finalOrders = ans.drop(1).zipWithIndex.map { case (o, idx) => (orders(o - 1), idx) }
            sortedOrders = finalOrders.sortBy { case (order, idx) => idx }
        } yield (sortedOrders)
    }
}


