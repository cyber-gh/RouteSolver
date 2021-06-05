package repositories.routes

import models.{DeliveryOrderModel, DeliveryRouteModel}

import scala.concurrent.Future

trait DeliveryRouteRepository {
    def getRoutes(supplierId: String): Future[List[DeliveryRouteModel]]

    def getRoute(idx: String): Future[Option[DeliveryRouteModel]]

    def addRoute(supplierId: String, name: String, startAddress: String, roundTrip: Boolean): Future[DeliveryRouteModel]

    def deleteRoute(routeId: String): Future[Boolean]

    def addOrder(routeId: String, address: String): Future[DeliveryOrderModel]

    def deleteOrder(orderId: String): Future[Boolean]


    def getOrders(routeId: String): Future[List[DeliveryOrderModel]]

    def getOrder(orderId: String): Future[Option[DeliveryOrderModel]]
}
