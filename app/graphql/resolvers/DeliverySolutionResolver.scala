package graphql.resolvers

import com.google.inject.Inject
import models.VRPAlg.VRPAlg
import models.{DeliveryOrderSolution, RouteDirections, RouteSolution}
import repositories.solver.RouteSolutionManager

import scala.concurrent.Future

class DeliverySolutionResolver @Inject()(repo: RouteSolutionManager) {

    def getSolution(idx: Option[String]): Future[Option[RouteSolution]] = idx match {
        case Some(value) => repo.getSolution(value)
        case None => Future.successful(None)
    }

    def getSolutions(routeId: String): Future[List[RouteSolution]] = repo.getAllSolutions(routeId)

    def getSolutionDetails(solutionId: String): Future[List[DeliveryOrderSolution]] = repo.getDeliveryOrders(solutionId)

    def solveRoute(routeId: String, algorithm: VRPAlg): Future[RouteSolution] = repo.solveRoute(routeId, algorithm)

    def deleteSolution(idx: String): Future[Boolean] = repo.removeSolution(idx)

    def setRouteSolution(routeId: String, solutionId: Option[String]): Future[Option[RouteSolution]] = repo.selectSolution(routeId, solutionId)

    def setBestSolution(routeId: String): Future[Option[RouteSolution]] = repo.selectBestSolution(routeId)

    def getDirections(idx: Option[String]): Future[Option[RouteDirections]] = idx match {
        case Some(value) => repo.getDirections(value)
        case None => Future.successful(None)
    }
}
