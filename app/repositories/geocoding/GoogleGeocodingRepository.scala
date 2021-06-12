package repositories.geocoding

import com.google.inject.Inject
import com.google.maps.model.LatLng
import com.google.maps.{GeoApiContext, GeocodingApi}
import errors.ExternalApiError

import scala.concurrent.{ExecutionContext, Future}

class GoogleGeocodingRepository @Inject()(implicit val executionContext: ExecutionContext) extends GeocodingRepository {
    private val apiKey = scala.util.Properties.envOrElse("GOOGLE_MAPS_GEOCODING_KEY", "")

    private val ctx = new GeoApiContext.Builder().apiKey(apiKey).build()

    override def geocode(address: String): Future[(Double, Double)] = Future {
        val result = GeocodingApi.geocode(ctx, address).await().headOption
        if (result.isEmpty) throw ExternalApiError(s"Cannot geocode ${address}")
        (result.get.geometry.location.lat, result.get.geometry.location.lng)
    }

    override def revGeocode(lat: Double, lng: Double): Future[String] = Future {
        val result = GeocodingApi.reverseGeocode(ctx, new LatLng(lat, lng)).await().head
        result.formattedAddress
    }
}
