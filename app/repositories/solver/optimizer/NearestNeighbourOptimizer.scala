package repositories.solver.optimizer

import com.google.inject.Inject
import models.Location
import repositories.solver.distance.DistanceRepository
import repositories.solver.utility.{DeliveryOrder, OptimizeSolution}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}


class NearestNeighbourAlg(val distanceMatrix: Array[Array[Double]], val nr: Int) {

    def run(): (List[Int], Double) = {
        val visited = Range(0, nr).toArray.map(x => false)
        visited(0) = true
        var currentNode = 0
        var cost: Double = 0
        val ans: ListBuffer[Int] = ListBuffer(0)
        for (x <- 1 until nr) {
            val nxt = Range(1, nr).filter(x => !visited(x)).minByOption(x => distanceMatrix(currentNode)(x))

            visited(nxt.get) = true
            cost += distanceMatrix(currentNode)(nxt.get)
            currentNode = nxt.get
            ans.addOne(currentNode)
        }

        return (ans.toList, cost)
    }
}

class NearestNeighbourOptimizer @Inject()(
                                             distanceRepository: DistanceRepository,
                                             implicit val executionContext: ExecutionContext
                                         ) extends RouteOptimizer {
    override def optimize(start: Location, orders: List[DeliveryOrder]): Future[OptimizeSolution] = {
        val locations = List(start) ++ orders.map(_.location)
        return for {
            distanceMatrix <- distanceRepository.getDistanceMatrix(locations).map(_.distances)
            optimizer = new NearestNeighbourAlg(distanceMatrix, locations.length)
            (ans, cost) = optimizer.run()
            finalOrders = ans.drop(1).zipWithIndex.map { case (o, idx) => (orders(o - 1), idx) }
            sortedOrders = finalOrders.sortBy { case (order, idx) => idx }
            sol = OptimizeSolution(sortedOrders, cost, (cost / 1000) / distanceRepository.averageSpeed * 3600)
        } yield sol
    }
}
