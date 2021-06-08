package repositories.routes

import akka.http.scaladsl.model.DateTime
import com.google.inject.Inject
import database.AppDatabase
import errors.{EntityNotFound, OperationNotPermitted}
import models._
import repositories.geocoding.GeocodingRepository
import slick.lifted.TableQuery

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DeliveryRouteRepositoryImpl @Inject()(
                                               val database: AppDatabase,
                                               val geocoding: GeocodingRepository,
                                               //                                               val clientsRepository: ClientsRepository,
                                               implicit val executionContext: ExecutionContext
                                           ) extends DeliveryRouteRepository with LocationRepository {
    val profile = database.profile
    /**
     * Specific database profile
     */
    private val db = database.db
    private val routesTable = TableQuery[models.DeliveryRouteModel.Table]
    private val ordersTable = TableQuery[models.DeliveryOrderModel.Table]
    private val locationsTable = TableQuery[models.Location.Table]
    private val solutionsTable = TableQuery[models.RouteSolution.Table]

    override def getRoutes(supplierId: String): Future[List[DeliveryRouteModel]] = db.run {
        Actions.getRoutes(supplierId)
    }

    override def getLocations(addresses: List[String]): Future[List[Location]] =
        Future.sequence(addresses.map(x => getLocation(x)))

    override def getLocation(lat: Double, lng: Double): Future[Location] = for {
        address <- geocoding.revGeocode(lat, lng)
        location = Location(address, lat, lng)
        _ <- db.run {
            Actions.addLocation(location)
        }
    } yield location

    override def addRoute(supplierId: String, name: String, startAddress: String, roundTrip: Boolean): Future[DeliveryRouteModel] = for {
        location <- getLocation(startAddress)
        route <- db.run {
            Actions.addRoute(
                DeliveryRouteModel(UUID.randomUUID().toString,
                    supplierId = supplierId,
                    name = name,
                    startLocationId = startAddress,
                    startTime = DateTime.now,
                    state = RouteState.Idle,
                    roundTrip = roundTrip,
                    selectedSolutionId = None
                ),
                location
            )
        }
    } yield route

    override def getLocation(address: String): Future[Location] = for {
        cachedLocation <- db.run {
            Actions.getLocation(address)
        }
        t <- cachedLocation match {
            case Some(value) => Future.successful(value)
            case None => for {
                (lat, lng) <- geocoding.geocode(address)
                l = Location(address, lat, lng)
                _ <- db.run {
                    Actions.addLocation(l)
                }
            } yield l
        }
    } yield t

    override def getRoute(idx: String): Future[Option[DeliveryRouteModel]] = db.run {
        Actions.getRoute(idx)
    }

    override def addOrderByClient(routeId: String, clientId: String): Future[DeliveryOrderModel] = ???
    //    for {
    //        maybeClient <- clientsRepository.getClient(clientId)
    //        client <- maybeClient match {
    //            case Some(value) => Future.successful(value)
    //            case None => Future.failed(EntityNotFound(s"No such client ${clientId}"))
    //        }
    //        o <- db.run{
    //            Actions.addOrder(
    //               DeliveryOrderModel(
    //                   UUID.randomUUID().toString,
    //                   client.name,
    //                   routeId,
    //                   client.locationId,
    //                   Some(clientId),
    //                   client.startTime,
    //                   client.endTime,
    //                   client.weight,
    //                   client.volume
    //
    //               )
    //
    //            )
    //        }
    //    } yield o

    override def deleteRoute(routeId: String): Future[Boolean] = for {
        maybeRoute <- getRoute(routeId)
        route <- maybeRoute match {
            case Some(value) => Future.successful(value)
            case None => Future.failed(EntityNotFound("No such route"))
        }
        result <- route.state match {
            case RouteState.Idle => db.run {
                Actions.deleteRoute(routeId)
            }
            case _ => Future.failed(OperationNotPermitted(s"Cannot delete route in state ${route.state.toString}"))
        }
    } yield result

    override def addOrder(orderForm: DeliveryOrderInputForm): Future[DeliveryOrderModel] = for {
        location <- getLocation(orderForm.address)
        order <- db.run {
            Actions.addOrder(
                DeliveryOrderModel(UUID.randomUUID().toString,
                    orderForm.name,
                    orderForm.routeId,
                    location.address,
                    orderForm.clientId,
                    orderForm.startTime,
                    orderForm.endTime,
                    orderForm.weight,
                    orderForm.volume
                )
            )
        }
    } yield order

    override def addOrder(routeId: String, address: String, name: String): Future[DeliveryOrderModel] = for {
        location <- getLocation(address)
        order <- db.run {
            Actions.addOrder(
                DeliveryOrderModel(UUID.randomUUID().toString,
                    name,
                    routeId,
                    location.address,
                    None, None, None, None, None
                )
            )
        }
    } yield order

    override def deleteOrder(orderId: String): Future[Boolean] = for {
        maybeOrder <- getOrder(orderId)
        order <- maybeOrder match {
            case Some(value) => Future.successful(value)
            case None => Future.failed(EntityNotFound(s"No such order ${orderId}"))
        }
        hasSolutions <- db.run {
            Actions.hasSolutions(order.routeId)
        }
        _ <- if (hasSolutions) throw OperationNotPermitted("You must delete all the solutions first")
        else db.run {
            Actions.deleteOrder(orderId)
        }
    } yield true

    override def getOrders(routeId: String): Future[List[DeliveryOrderModel]] = db.run {
        Actions.getOrders(routeId)
    }

    override def getOrder(orderId: String): Future[Option[DeliveryOrderModel]] = db.run {
        Actions.getOrder(orderId)
    }

    import profile.api._

    object Actions {
        def addLocation(location: Location): DBIO[Location] = for {
            inserted <- locationsTable.insertOrUpdate(location).map(_ => location)
        } yield inserted

        def getLocation(id: String): DBIO[Option[Location]] = for {
            location <- locationsTable.filter(_.address === id).result.headOption
        } yield location

        def getOrders(routeId: String): DBIO[List[DeliveryOrderModel]] = for {
            orders <- ordersTable.filter(_.routeId === routeId).result
        } yield orders.toList

        def deleteOrder(orderId: String): DBIO[Boolean] = for {
            maybeDelete <- ordersTable.filter(_.id === orderId).delete
            isDeleted = if (maybeDelete == 1) true else false
        } yield isDeleted

        def addOrder(order: DeliveryOrderModel): DBIO[DeliveryOrderModel] = for {
            _ <- ordersTable.insertOrUpdate(order)
        } yield order

        def getOrder(orderId: String): DBIO[Option[DeliveryOrderModel]] = ordersTable.filter(_.id === orderId).result.headOption

        def deleteRoute(routeId: String): DBIO[Boolean] = for {
            t <- ordersTable.filter(_.routeId === routeId).delete
            maybeDeleted <- routesTable.filter(_.id === routeId).delete
            isDeleted = if (maybeDeleted == 1) true else false
        } yield isDeleted

        def getRoute(routeId: String): DBIO[Option[DeliveryRouteModel]] = routesTable.filter(_.id === routeId).result.headOption

        def addRoute(route: DeliveryRouteModel, location: Location): DBIO[DeliveryRouteModel] = for {
            locationId <- locationsTable.insertOrUpdate(location).map(_ => location.address)
            insertedRoute <- routesTable.insertOrUpdate(route.copy(startLocationId = locationId)).map(_ => route)
        } yield insertedRoute

        def getRoutes(supplierId: String): DBIO[List[DeliveryRouteModel]] = for {
            routes <- routesTable.filter(_.supplierId === supplierId).result
        } yield routes.toList

        def hasSolutions(routeId: String): DBIO[Boolean] = for {
            solutions <- solutionsTable.filter(_.routeId === routeId).length.result
        } yield solutions > 0
    }
}
