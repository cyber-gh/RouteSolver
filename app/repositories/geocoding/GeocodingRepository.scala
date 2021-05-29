package repositories.geocoding

import scala.concurrent.Future

trait GeocodingRepository {

    def geocode(address: String): Future[(Double, Double)]

    def revGeocode(lat: Double, lng: Double): Future[String]
}
