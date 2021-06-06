package graphql.resolvers

import com.google.inject.Inject
import models.VRPAlg.VRPAlg
import models.{DeliveryOrderSolution, RouteSolution}
import repositories.solver.RouteSolutionManager

import scala.concurrent.Future

class DeliverySolutionResolver @Inject()(repo: RouteSolutionManager) {

    def getSolutions(routeId: String): Future[List[RouteSolution]] = repo.getAllSolutions(routeId)

    def getSolutionDetails(solutionId: String): Future[List[DeliveryOrderSolution]] = repo.getDeliveryOrders(solutionId)

    def solveRoute(routeId: String, algorithm: VRPAlg): Future[RouteSolution] = repo.solveRoute(routeId, algorithm)

    def deleteSolution(idx: String): Future[Boolean] = repo.removeSolution(idx)
}
