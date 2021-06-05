package repositories.solver

import akka.http.scaladsl.model.DateTime
import com.google.inject.Inject
import database.AppDatabase
import models.{DeliveryOrderSolution, RouteSolution}
import repositories.routes.{DeliveryRouteRepository, LocationRepository}
import repositories.solver.optimizer.RouteOptimizer
import slick.lifted.TableQuery

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class RouteSolutionManagerImpl @Inject()(
                                            private val deliveryRouteRepository: DeliveryRouteRepository,
                                            private val optimizer: RouteOptimizer,
                                            private val locationRepository: LocationRepository,
                                            private val database: AppDatabase,
                                            implicit val executionContext: ExecutionContext) extends RouteSolutionManager {
    val profile = database.profile
    /**
     * Specific database profile
     */
    private val db = database.db
    private val routesTable = TableQuery[models.DeliveryRouteModel.Table]
    private val ordersTable = TableQuery[models.DeliveryOrderModel.Table]
    private val solutionsTable = TableQuery[models.RouteSolution.Table]
    private val solutionOrdersTable = TableQuery[models.DeliveryOrderSolution.Table]

    override def solveRoute(routeId: String, algorithm: String): Future[RouteSolution] = for {
        (sol, orders) <- for {
            locationId <- deliveryRouteRepository.getRoute(routeId).map(x => x.get.startLocationId)
            startLocation <- locationRepository.getLocation(locationId)
            orders <- deliveryRouteRepository.getOrders(routeId)
            locations <- locationRepository.getLocations(orders.map(_.locationId))
            fullOrders = orders.map(x => x.toFullOrder(locations.find(_.address == x.locationId).get))
            fullSolution <- optimizer.optimize(startLocation, fullOrders)

            ordersSolution = fullSolution.orders
            routeSolution = RouteSolution(UUID.randomUUID().toString, routeId, algorithm, orders.length, fullSolution.cost, 0, None, None)
            solutionOrders = ordersSolution.map { case (delivery, orderOf) => DeliveryOrderSolution(UUID.randomUUID().toString, routeSolution.id, delivery.id, orderOf, DateTime.now, DateTime.now + (5 * 60 * 1000)) }
        } yield (routeSolution, solutionOrders)
        _ <- db.run {
            Actions.addSolution(sol)
        }
        _ <- db.run {
            Actions.addDetailedSolution(orders)
        }
    } yield sol

    override def getAllSolutions(routeId: String): Future[List[RouteSolution]] = db.run {
        Actions.getSolutions(routeId)
    }

    override def getSolution(idx: String): Future[Option[RouteSolution]] = db.run {
        Actions.getSolution(idx)
    }

    override def getDeliveryOrders(solutionId: String): Future[List[DeliveryOrderSolution]] = db.run {
        Actions.getDetailedSolution(solutionId)
    }

    override def removeSolution(solutionId: String): Future[Boolean] = db.run {
        Actions.removeSolution(solutionId)
    }

    import profile.api._

    object Actions {
        def addSolution(solution: RouteSolution): DBIO[RouteSolution] = for {
            inserted <- solutionsTable.insertOrUpdate(solution).map(_ => solution)
        } yield inserted

        def addDetailedSolution(orders: List[DeliveryOrderSolution]): DBIO[Unit] = for {
            _ <- solutionOrdersTable ++= orders
        } yield ()

        def getDetailedSolution(solutionId: String): DBIO[List[DeliveryOrderSolution]] = for {
            orders <- solutionOrdersTable.filter(_.solutionId === solutionId).result
        } yield orders.toList

        def getSolution(idx: String): DBIO[Option[RouteSolution]] = solutionsTable.filter(_.id === idx).result.headOption

        def getSolutions(routeId: String): DBIO[List[RouteSolution]] = for {
            solutions <- solutionsTable.filter(_.routeId === routeId).result
        } yield solutions.toList

        def removeSolution(solutionId: String): DBIO[Boolean] = for {
            _ <- solutionOrdersTable.filter(_.solutionId === solutionId).delete
            _ <- solutionsTable.filter(_.id === solutionId).delete
        } yield true

    }

}
