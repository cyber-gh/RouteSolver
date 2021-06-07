package repositories.clients

import models.DeliveryClient

import scala.concurrent.Future

trait ClientsRepository {

    def create(name: String, email: String, lat: Double, lng: Double, supplierId: String): Future[DeliveryClient]

    def create(
                  name: String,
                  email: String,
                  address: String,
                  supplierId: String,
                  startTime: Option[String] = None,
                  endTime: Option[String] = None,
                  weight: Option[Double] = None,
                  volume: Option[Double] = None
              ): Future[DeliveryClient]

    def getClients(supplierId: String): Future[List[DeliveryClient]]

    def delete(idx: String): Future[Boolean]
}
