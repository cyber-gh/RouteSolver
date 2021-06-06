package repositories.solver

import models.VRPAlg.VRPAlg
import models.{DeliveryOrderSolution, RouteSolution}

import scala.concurrent.Future

trait RouteSolutionManager {

    def solveRoute(routeId: String, algorithm: VRPAlg): Future[RouteSolution]

    def getAllSolutions(routeId: String): Future[List[RouteSolution]]

    def getSolution(idx: String): Future[Option[RouteSolution]]

    def getDeliveryOrders(solutionId: String): Future[List[DeliveryOrderSolution]]

    def removeSolution(solutionId: String): Future[Boolean]


}
