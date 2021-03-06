package repositories.routes

import models.{DeliveryOrderInputForm, DeliveryOrderModel, DeliveryRouteModel}

import scala.concurrent.Future

trait DeliveryRouteRepository {
    def getRoutes(supplierId: String): Future[List[DeliveryRouteModel]]

    def getRoute(idx: String): Future[Option[DeliveryRouteModel]]

    def addRoute(supplierId: String, name: String, startAddress: String, roundTrip: Boolean): Future[DeliveryRouteModel]

    def deleteRoute(routeId: String): Future[Boolean]

    def addOrder(routeId: String, address: String, name: String): Future[DeliveryOrderModel]

    def deleteOrder(orderId: String): Future[Boolean]

    def getOrders(routeId: String): Future[List[DeliveryOrderModel]]

    def getOrder(orderId: String): Future[Option[DeliveryOrderModel]]

    def addOrder(orderForm: DeliveryOrderInputForm): Future[DeliveryOrderModel]

    def addOrderByClient(routeId: String, clientId: String): Future[DeliveryOrderModel]

    def assignDriverToRoute(routeId: String, driverId: String): Future[Boolean]

    def getDriverAssignedRoutes(driverId: String): Future[List[DeliveryRouteModel]]
}
