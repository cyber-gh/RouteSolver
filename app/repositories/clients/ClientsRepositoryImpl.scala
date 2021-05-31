package repositories.clients

import com.google.inject.Inject
import database.AppDatabase
import models.DeliveryClient
import repositories.geocoding.GeocodingRepository
import repositories.routes.LocationRepository
import slick.lifted.TableQuery

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ClientsRepositoryImpl @Inject()(val database: AppDatabase,
                                      val geocoding: GeocodingRepository,
                                      val locationRepository: LocationRepository,
                                      implicit val executionContext: ExecutionContext) extends ClientsRepository {

    val profile = database.profile
    /**
     * Specific database profile
     */
    private val db = database.db
    private val clientsTable = TableQuery[DeliveryClient.Table]

    override def create(name: String, email: String, address: String, supplierId: String): Future[DeliveryClient] = for {
        location <- locationRepository.getLocation(address)
        client = DeliveryClient(UUID.randomUUID().toString, name, email, locationId = location.address, supplierId = Some(supplierId))
        insertedClient <- db.run {
            Actions.addClient(client)
        }
    } yield insertedClient

    override def create(name: String, email: String, lat: Double, lng: Double, supplierId: String): Future[DeliveryClient] = for {
        location <- locationRepository.getLocation(lat, lng)
        client <- db.run {
            Actions.addClient(
                DeliveryClient(UUID.randomUUID().toString, name, email, locationId = location.address, supplierId = Some(supplierId))
            )
        }
    } yield client

    override def getClients(supplierId: String): Future[List[DeliveryClient]] = db.run {
        Actions.getClients(supplierId)
    }

    override def delete(idx: String): Future[Boolean] = db.run {
        Actions.deleteClient(idx)
    }

    import profile.api._

    object Actions {
        def addClient(client: DeliveryClient): DBIO[DeliveryClient] = for {
            insertedClient <- clientsTable.insertOrUpdate(client).map(_ => client)
        } yield insertedClient

        def getClients(supplierId: String): DBIO[List[DeliveryClient]] = for {
            clients <- clientsTable.filter(_.supplierId === supplierId).result
        } yield clients.toList

        def deleteClient(idx: String): DBIO[Boolean] = for {
            maybeDeleted <- clientsTable.filter(_.id === idx).delete
            isDeleted = if (maybeDeleted == 1) true else false
        } yield isDeleted
    }
}
