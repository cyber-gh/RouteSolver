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
        val weightConstraints = orders.map(x => x.weight.getOrElse(0.0))
        val volumeConstraints = orders.map(x => x.volume.getOrElse(0.0))
        val timeConstraints = orders.map(x => extractTimeWindow(x.startTime, x.endTime))
        return for {
            matrix <- distanceRepository.getDistanceMatrix(locations)
            (ans, cost) = optimize(matrix.distances, matrix.travelTimes, locations.length, weightConstraints, volumeConstraints, timeConstraints)
            finalOrders = ans.zipWithIndex.map { case (o, idx) => (orders(o - 1), idx) }
            sortedOrders = finalOrders.sortBy { case (order, idx) => idx }
            sol = OptimizeSolution(sortedOrders, cost, (cost / 1000) / distanceRepository.averageSpeed * 3600)
        } yield sol
    }

    private def extractTime(tm: Option[String], default: String = "00:00"): Double = {
        val st = tm.getOrElse(default)
        val startH = st.split(":")(0).toInt
        val startM = st.split(":")(1).toInt
        val start = startH * 3600 + startM * 60


        return start
    }

    private def extractTimeWindow(startTime: Option[String], endTime: Option[String]): (Double, Double) = {
        return (extractTime(startTime, "00:00"), extractTime(endTime, "23:59"))
    }

    private def optimize(distances: Array[Array[Double]],
                         travelTimes: Array[Array[Double]],
                         nr: Int,
                         weightConstraints: List[Double] = List(),
                         volumeConstraints: List[Double] = List(),
                         timeWindowConstraints: List[(Double, Double)] = List()
                        ): (List[Int], Double) = {
        import com.graphhopper.jsprit.core.problem.vehicle.{VehicleImpl, VehicleTypeImpl}
        val typeBuilder = VehicleTypeImpl.Builder.newInstance("vehicle-type")
        typeBuilder.setCostPerDistance(1.0)
        typeBuilder.setCostPerTransportTime(0.0)
        typeBuilder.addCapacityDimension(0, 200) // 0 is weight
        typeBuilder.addCapacityDimension(1, 200) // 1 is volume

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
                matrixBuilder.addTransportTime(x, y, travelTimes(x)(y))
            }
        }
        vrpBuilder.setRoutingCost(matrixBuilder.build())


        for (t <- 1 until nr) {
            val delivery = Service.Builder.newInstance(t.toString)
                .setLocation(JLocation.Builder.newInstance().setIndex(t).build())
                .addSizeDimension(0, weightConstraints(t - 1).toInt)
                .addSizeDimension(1, volumeConstraints(t - 1).toInt)
                .addTimeWindow(timeWindowConstraints(t - 1)._1, timeWindowConstraints(t - 1)._2)
                .build()
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
