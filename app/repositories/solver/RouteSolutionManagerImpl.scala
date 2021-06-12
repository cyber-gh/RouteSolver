package repositories.solver

import akka.http.scaladsl.model.DateTime
import cats.data.OptionT
import com.google.inject.Inject
import database.AppDatabase
import errors.EntityNotFound
import models.VRPAlg.VRPAlg
import models._
import repositories.directions.DirectionsRepository
import repositories.locations.LocationRepository
import repositories.routes.DeliveryRouteRepository
import repositories.solver.optimizer.{BasicRouteOptimizer, ChristofidesOptimizer, JspritOptimizer, NearestNeighbourOptimizer}
import slick.lifted.TableQuery

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class RouteSolutionManagerImpl @Inject()(
                                            private val deliveryRouteRepository: DeliveryRouteRepository,
                                            private val backtrackOptimizer: BasicRouteOptimizer,
                                            private val nnOptimizer: NearestNeighbourOptimizer,
                                            private val jspritOptimizer: JspritOptimizer,
                                            private val chOptimizer: ChristofidesOptimizer,
                                            private val locationRepository: LocationRepository,
                                            private val directionsRepository: DirectionsRepository,
                                            private val database: AppDatabase,
                                            implicit val executionContext: ExecutionContext) extends RouteSolutionManager {
    val profile = database.profile
    /**
     * Specific database profile
     */
    private val db = database.db
    private val routesTable = TableQuery[models.DeliveryRouteModel.Table]
    private val solutionsTable = TableQuery[models.RouteSolution.Table]
    private val solutionOrdersTable = TableQuery[models.DeliveryOrderSolution.Table]
    private val ordersTable = TableQuery[models.DeliveryOrderModel.Table]
    private val locationsTable = TableQuery[models.Location.Table]
    private val directionsTable = TableQuery[models.RouteDirections.Table]

    import profile.api._

    override def getDirections(idx: String): Future[Option[RouteDirections]] = for {
        directions <- db.run(Actions.getDirections(idx))
    } yield directions

    private def setDirections(route: DeliveryRouteModel, solution: RouteSolution): Future[Unit] = for {
        startLocation <- locationRepository.getLocation(route.startLocationId)
        combined <- db.run {
            Actions.getSolutionLocationsOrder(solution.id).result
        }
        locations = List(startLocation) ++ combined.sortBy(_._2).map(_._1)
        directions <- directionsRepository.calculateDirections(locations)
        _ <- db.run {
            Actions.addDirections(directions)
        }
        s = solution.copy(directionsId = Some(directions.id))
        _ <- db.run {
            Actions.addSolution(s)
        }

    } yield ()

    override def selectSolution(routeId: String, solutionId: String): Future[Option[RouteSolution]] = (for {
        maybeRoute <- OptionT(db.run {
            Actions.getRoute(routeId)
        })
        //        route <- maybeRoute match {
        //            case Some(value) => Future.successful(value)
        //            case None => Future.failed(EntityNotFound("No such route"))
        //        }
        maybeSolution <- OptionT(getSolution(solutionId))
        //        solution <- maybeSolution match {
        //            case Some(value) => Future.successful(value)
        //            case None => Future.failed(EntityNotFound("No such solution"))
        //        }
        t <- OptionT(db.run {
            Actions.updateRouteSolution(maybeRoute.id, Some(solutionId))
        }.map(x => Some(x)))
    } yield maybeSolution).value

    override def selectBestSolution(routeId: String): Future[Option[RouteSolution]] = for {
        solution <- getAllSolutions(routeId).map(x => x.minByOption(it => it.distance))
        sol <- solution match {
            case Some(value) => selectSolution(routeId, value.id)
            case None => Future.failed(EntityNotFound("No solutions availalbe"))
        }
    } yield sol

    override def solveRoute(routeId: String, algorithm: VRPAlg): Future[RouteSolution] = for {
        s <- db.run {
            Actions.getSolution(routeId, algorithm)
        }
        ans <- s match {
            case Some(x) => Future.successful(x)
            case None => for {
                (sol, orders) <- solve(routeId, algorithm)

                _ <- db.run {
                    Actions.addSolution(sol)
                }
                _ <- db.run {
                    Actions.addDetailedSolution(orders)
                }
            } yield sol
        }
        route <- deliveryRouteRepository.getRoute(routeId)
        _ <- setDirections(route.get, ans)


    } yield ans

    private def solve(routeId: String, algorithm: VRPAlg) = {
        for {
            locationId <- deliveryRouteRepository.getRoute(routeId).map(x => x.get.startLocationId)
            startLocation <- locationRepository.getLocation(locationId)
            orders <- deliveryRouteRepository.getOrders(routeId)
            locations <- locationRepository.getLocations(orders.map(_.locationId))
            fullOrders = orders.map(x => x.toFullOrder(locations.find(_.address == x.locationId).get))
            optimizer = algorithm match {
                case VRPAlg.backtrack => backtrackOptimizer
                case VRPAlg.nearestNeighbour => nnOptimizer
                case VRPAlg.greedySchrimp => jspritOptimizer
                case VRPAlg.Christofides => chOptimizer
            }
            fullSolution <- optimizer.optimize(startLocation, fullOrders)

            ordersSolution = fullSolution.orders
            routeSolution = RouteSolution(UUID.randomUUID().toString, routeId, None, algorithm, orders.length, fullSolution.distance, fullSolution.time, None, None)
            solutionOrders = ordersSolution.map { case (delivery, orderOf) => DeliveryOrderSolution(UUID.randomUUID().toString, routeSolution.id, delivery.id, orderOf, DateTime.now, DateTime.now + (5 * 60 * 1000)) }
        } yield (routeSolution, solutionOrders)
    }

    override def getAllSolutions(routeId: String): Future[List[RouteSolution]] = db.run {
        Actions.getSolutions(routeId)
    }

    override def getSolution(idx: String): Future[Option[RouteSolution]] = db.run {
        Actions.getSolution(idx)
    }

    override def getDeliveryOrders(solutionId: String): Future[List[DeliveryOrderSolution]] = db.run {
        Actions.getDetailedSolution(solutionId)
    }

    override def removeSolution(solutionId: String): Future[Boolean] = for {
        maybeSolution <- getSolution(solutionId)
        solution <- maybeSolution match {
            case Some(value) => Future.successful(value)
            case None => Future.failed(EntityNotFound("No such solution"))
        }
        maybeRoute <- db.run {
            Actions.getRouteBySolution(solution.id)
        }
        _ <- maybeRoute match {
            case Some(value) => db.run {
                Actions.updateRouteSolution(value.id, None)
            }
            case None => Future.successful()
        }
        _ <- db.run {
            Actions.removeSolution(solutionId)
        }
    } yield true


    object Actions {
        private implicit val algorithmColumnType = MappedColumnType.base[VRPAlg, String](
            state => state.toString,
            {
                case "backtrack" => VRPAlg.backtrack
                case "nearest_neighbour" => VRPAlg.nearestNeighbour
                case "GreedySchrimp" => VRPAlg.greedySchrimp
                case "Christofides" => VRPAlg.Christofides
                case _ => VRPAlg.unknown
            }
        )

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

        def getRoute(idx: String): DBIO[Option[DeliveryRouteModel]] = routesTable.filter(_.id === idx).result.headOption

        def getSolution(routeId: String, algorithm: VRPAlg): DBIO[Option[RouteSolution]] =
            solutionsTable.filter(x => x.routeId === routeId && x.algorithm === algorithm).result.headOption

        def getSolutions(routeId: String): DBIO[List[RouteSolution]] = for {
            solutions <- solutionsTable.filter(_.routeId === routeId).result
        } yield solutions.toList

        def removeSolution(solutionId: String): DBIO[Boolean] = for {
            _ <- solutionOrdersTable.filter(_.solutionId === solutionId).delete
            _ <- solutionsTable.filter(_.id === solutionId).delete
        } yield true

        def updateRouteSolution(routeId: String, solutionId: Option[String]): DBIO[Boolean] = for {
            route <- routesTable.filter(_.id === routeId).result.head
            _ <- routesTable.insertOrUpdate(route.copy(selectedSolutionId = solutionId))
        } yield true

        def getSolutionLocationsOrder(solutionId: String) = for {
            s <- solutionOrdersTable if s.solutionId === solutionId
            o <- ordersTable if s.orderId === o.id
            l <- locationsTable if o.locationId === l.address
        } yield (l, s.order)

        def addDirections(directions: RouteDirections) = for {
            _ <- directionsTable.insertOrUpdate(directions)
        } yield ()

        def getDirections(idx: String): DBIO[Option[RouteDirections]] = directionsTable.filter(_.id === idx).result.headOption

        def getRouteBySolution(selectedSolutionId: String): DBIO[Option[DeliveryRouteModel]] = routesTable.filter(_.selectedSolutionId === selectedSolutionId).result.headOption

    }

}
