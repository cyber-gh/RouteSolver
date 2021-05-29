package repositories.geocoding

import com.google.inject.Inject
import com.google.maps.model.LatLng
import com.google.maps.{GeoApiContext, GeocodingApi}

import scala.concurrent.{ExecutionContext, Future}

class GoogleGeocodingRepository @Inject()(implicit val executionContext: ExecutionContext) extends GeocodingRepository {
    private val apiKey = scala.util.Properties.envOrElse("GOOGLE_MAPS_GEOCODING_KEY", "")

    private val ctx = new GeoApiContext.Builder().apiKey(apiKey).build()

    override def geocode(address: String): Future[(Double, Double)] = Future {
        val result = GeocodingApi.geocode(ctx, address).await().head
        (result.geometry.location.lat, result.geometry.location.lng)
    }

    override def revGeocode(lat: Double, lng: Double): Future[String] = Future {
        val result = GeocodingApi.reverseGeocode(ctx, new LatLng(lat, lng)).await().head
        result.formattedAddress
    }
}
