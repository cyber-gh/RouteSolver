package repositories.solver

import models.VRPAlg.VRPAlg
import models.{DeliveryOrderSolution, RouteDirections, RouteSolution}

import scala.concurrent.Future

trait RouteSolutionManager {

    def solveRoute(routeId: String, algorithm: VRPAlg): Future[RouteSolution]

    def getAllSolutions(routeId: String): Future[List[RouteSolution]]

    def getSolution(idx: String): Future[Option[RouteSolution]]

    def getDeliveryOrders(solutionId: String): Future[List[DeliveryOrderSolution]]

    def removeSolution(solutionId: String): Future[Boolean]

    def selectSolution(routeId: String, solutionId: String): Future[Option[RouteSolution]]

    def selectBestSolution(routeId: String): Future[Option[RouteSolution]]

    def getDirections(idx: String): Future[Option[RouteDirections]]


}
