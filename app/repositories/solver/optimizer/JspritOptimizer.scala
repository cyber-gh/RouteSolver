package repositories.solver.optimizer

import com.google.inject.Inject
import com.graphhopper.jsprit.core.algorithm.box.GreedySchrimpfFactory
import com.graphhopper.jsprit.core.algorithm.termination.TimeTermination
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem.FleetSize
import com.graphhopper.jsprit.core.problem.job.Service
import com.graphhopper.jsprit.core.problem.{VehicleRoutingProblem, Location => JLocation}
import com.graphhopper.jsprit.core.util.{FastVehicleRoutingTransportCostsMatrix, Solutions}
import models.Location
import play.api.Logger
import repositories.solver.distance.DistanceRepository
import repositories.solver.utility.{DeliveryOrder, OptimizeSolution}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

class JspritOptimizer @Inject()(distanceRepository: DistanceRepository, implicit val executionContext: ExecutionContext) extends RouteOptimizer {
    private val logger: Logger = Logger(this.getClass())

    override def optimize(start: Location, orders: List[DeliveryOrder]): Future[OptimizeSolution] = {
        val locations = List(start) ++ orders.map(_.location)
        return for {
            matrix <- distanceRepository.getDistanceMatrix(locations)
            (ans, cost) = optimize(matrix.distances, matrix.travelTimes, locations.length)
            finalOrders = ans.zipWithIndex.map { case (o, idx) => (orders(o - 1), idx) }
            sortedOrders = finalOrders.sortBy { case (order, idx) => idx }
            sol = OptimizeSolution(sortedOrders, cost)
        } yield sol
    }

    private def optimize(distances: Array[Array[Double]], travelTimes: Array[Array[Double]], nr: Int): (List[Int], Double) = {
        import com.graphhopper.jsprit.core.problem.vehicle.{VehicleImpl, VehicleTypeImpl}
        val typeBuilder = VehicleTypeImpl.Builder.newInstance("vehicle-type")
        typeBuilder.setCostPerDistance(1.0)
        typeBuilder.setCostPerTransportTime(0.0)
        val bigType = typeBuilder.build

        val vehicleBuilder = VehicleImpl.Builder.newInstance("vehicle")
        vehicleBuilder.setStartLocation(JLocation.Builder.newInstance.setIndex(0).build)
        vehicleBuilder.setType(bigType)
        val bigVehicle = vehicleBuilder.build


        val vrpBuilder = VehicleRoutingProblem.Builder.newInstance()
        vrpBuilder.addVehicle(bigVehicle)
        vrpBuilder.setFleetSize(FleetSize.INFINITE)

        val matrixBuilder = FastVehicleRoutingTransportCostsMatrix.Builder.newInstance(nr, false)
        for (x <- 0 until nr) {
            for (y <- 0 until nr) {
                matrixBuilder.addTransportDistance(x, y, distances(x)(y))
            }
        }
        vrpBuilder.setRoutingCost(matrixBuilder.build())


        for (t <- 1 until nr) {
            val delivery = Service.Builder.newInstance(t.toString)
                .setLocation(JLocation.Builder.newInstance().setIndex(t).build()).build()
            vrpBuilder.addJob(delivery)
        }

        val vrp = vrpBuilder.build()

        val alg = new GreedySchrimpfFactory().createAlgorithm(vrp)
        alg.setPrematureAlgorithmTermination(new TimeTermination(2000))
        val solutions = alg.searchSolutions()
        val bestSolution = Solutions.bestOf(solutions)

        val cost = bestSolution.getCost
        val t = bestSolution.getRoutes.asScala.toList.head
        val sol = t.getActivities.asScala.toList.map(x => x.getLocation.getIndex)


        //        throw AmbigousResult(t.getActivities().toString)
        //        throw AmbigousResult(sol.toString)
        return (sol, cost)

    }
}
