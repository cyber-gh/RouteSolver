package graphql.resolvers

import com.google.inject.Inject
import models.DeliveryClient
import repositories.clients.ClientsRepository

import scala.concurrent.Future

class ClientsResolver @Inject()(val clientsRepo: ClientsRepository) {

    def addClient(name: String, email: String, address: String, supplierId: String): Future[DeliveryClient] = clientsRepo.create(name, email, address, supplierId)

    def getClients(supplierId: String): Future[List[DeliveryClient]] = clientsRepo.getClients(supplierId)

    def removeClient(idx: String): Future[Boolean] = clientsRepo.delete(idx)
}
