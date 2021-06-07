package graphql.resolvers

import com.google.inject.Inject
import models.{DeliveryOrderModel, DeliveryRouteModel, Location}
import repositories.routes.{DeliveryRouteRepository, LocationRepository}

import scala.concurrent.Future


//TODO add permissions checks
class DeliveryResolver @Inject()(deliveryRouteRepository: DeliveryRouteRepository, locationRepository: LocationRepository) {

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

}
