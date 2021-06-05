package graphql.resolvers

import com.google.inject.Inject
import errors.UndefinedAlgorithm
import models.{DeliveryOrderSolution, RouteSolution}
import repositories.solver.RouteSolutionManager

import scala.concurrent.Future

class DeliverySolutionResolver @Inject()(repo: RouteSolutionManager) {

    def getSolutions(routeId: String): Future[List[RouteSolution]] = repo.getAllSolutions(routeId)

    def getSolutionDetails(solutionId: String): Future[List[DeliveryOrderSolution]] = repo.getDeliveryOrders(solutionId)

    def solveRoute(routeId: String, algorithm: String): Future[RouteSolution] = algorithm match {
        case "backtrack" => repo.solveRoute(routeId, algorithm)
        case _ => Future.failed(new UndefinedAlgorithm("No such algorithm is available"))
    }
}
