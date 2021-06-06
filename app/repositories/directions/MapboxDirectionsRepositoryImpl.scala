package repositories.directions

import com.google.inject.Inject
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.{DirectionsCriteria, MapboxDirections}
import com.mapbox.geojson.Point
import models.{Location, RouteDirections}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class MapboxDirectionsRepositoryImpl @Inject()(implicit val executionContext: ExecutionContext) extends DirectionsRepository {

    private val apiKey: String = scala.util.Properties.envOrElse("MAPBOX_API_KEY", "")

    override def calculateDirections(locations: List[Location]): Future[RouteDirections] = {
        val waypoints = locations.drop(1).dropRight(1).map(_.toPoint)

        val builder = MapboxDirections.builder()
            .origin(locations.head.toPoint)
            .destination(locations.last.toPoint)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .accessToken(apiKey)
        for (w <- waypoints) {
            builder.addWaypoint(w)
        }
        val client = builder.build()

        for {
            response <- client.callFuture
            route = response.routes().get(0)
            directions = RouteDirections(UUID.randomUUID().toString, route.geometry(), route.distance(), route.duration())
        } yield directions

    }

    implicit class PointConverter(location: Location) {
        def toPoint: Point = Point.fromLngLat(this.location.longitude, this.location.latitude)
    }

    implicit class RequestConverters(directionsClient: MapboxDirections) {
        def callFuture: Future[DirectionsResponse] = Future {
            val response = directionsClient.executeCall()
            response.body()
        }
    }
}
