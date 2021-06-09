package repositories.locations

import com.google.inject.Inject
import database.AppDatabase
import models.Location
import repositories.geocoding.GeocodingRepository
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

class CachedLocationsRepository @Inject()(
                                             database: AppDatabase,
                                             geocoding: GeocodingRepository,
                                             implicit val executionContext: ExecutionContext
                                         ) extends LocationRepository {

    val profile = database.profile
    /**
     * Specific database profile
     */
    private val db = database.db
    private val locationsTable = TableQuery[models.Location.Table]

    override def getLocations(addresses: List[String]): Future[List[Location]] =
        Future.sequence(addresses.map(x => getLocation(x)))

    override def getLocation(lat: Double, lng: Double): Future[Location] = for {
        address <- geocoding.revGeocode(lat, lng)
        location = Location(address, lat, lng)
        _ <- db.run {
            Actions.addLocation(location)
        }
    } yield location

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

    import profile.api._

    object Actions {
        def addLocation(location: Location): DBIO[Location] = for {
            inserted <- locationsTable.insertOrUpdate(location).map(_ => location)
        } yield inserted

        def getLocation(id: String): DBIO[Option[Location]] = for {
            location <- locationsTable.filter(_.address === id).result.headOption
        } yield location
    }

}
