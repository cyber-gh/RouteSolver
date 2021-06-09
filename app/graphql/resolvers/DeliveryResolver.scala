package graphql.resolvers

import com.google.inject.Inject
import models.{DeliveryOrderInputForm, DeliveryOrderModel, DeliveryRouteModel, Location}
import repositories.locations.LocationRepository
import repositories.routes.DeliveryRouteRepository

import scala.concurrent.{ExecutionContext, Future}


//TODO add permissions checks
class DeliveryResolver @Inject()(deliveryRouteRepository: DeliveryRouteRepository, locationRepository: LocationRepository, implicit val executionContext: ExecutionContext) {

    def getRoutes(supplierId: String): Future[List[DeliveryRouteModel]] = deliveryRouteRepository.getRoutes(supplierId)

    def getRoute(idx: String): Future[Option[DeliveryRouteModel]] = deliveryRouteRepository.getRoute(idx)

    def addRoute(supplierId: String, name: String, startAddress: String, roundTrip: Boolean): Future[DeliveryRouteModel]
    = deliveryRouteRepository.addRoute(supplierId, name, startAddress, roundTrip)

    def deleteRoute(routeId: String): Future[Boolean] = deliveryRouteRepository.deleteRoute(routeId)

    def addOrder(routeId: String, address: String, name: String): Future[DeliveryOrderModel] = deliveryRouteRepository.addOrder(routeId, address, name)

    def deleteOrder(orderId: String): Future[Boolean] = deliveryRouteRepository.deleteOrder(orderId)

    def getOrders(routeId: String): Future[List[DeliveryOrderModel]] = deliveryRouteRepository.getOrders(routeId)

    def getOrder(orderId: String): Future[Option[DeliveryOrderModel]] = deliveryRouteRepository.getOrder(orderId)


    def getLocation(address: String): Future[Location] = locationRepository.getLocation(address)


    def addOrder(orderForm: DeliveryOrderInputForm): Future[DeliveryOrderModel] = deliveryRouteRepository.addOrder(orderForm)

    def addOrderByClient(routeId: String, clientId: String): Future[DeliveryOrderModel] = deliveryRouteRepository.addOrderByClient(routeId, clientId)

    def addOrdersByClients(routeId: String, clientIds: List[String]): Future[List[DeliveryOrderModel]] = Future.sequence(clientIds.map(it => addOrderByClient(routeId, it)))

    def assignDriverToRoute(routeId: String, driverId: String): Future[Boolean] = deliveryRouteRepository.assignDriverToRoute(routeId, driverId)

    def getDriverAssignedRoutes(driverId: String): Future[List[DeliveryRouteModel]] = deliveryRouteRepository.getDriverAssignedRoutes(driverId)
}
