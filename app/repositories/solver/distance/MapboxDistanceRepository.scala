package repositories.solver.distance

import com.google.inject.Inject
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.matrix.v1.MapboxMatrix
import com.mapbox.geojson.Point
import models.Location

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

class MapboxDistanceRepository @Inject()(
                                            directDistanceRepository: DirectDistanceRepository,
                                            implicit val executionContext: ExecutionContext) extends DistanceRepository {

    private val apiKey: String = scala.util.Properties.envOrElse("MAPBOX_API_KEY", "")

    override def getDistanceMatrix(locations: List[Location]): Future[TimeDistanceResponse] = {
        if (locations.length <= 10) getRemoteDistance(locations)
        else directDistanceRepository.getDistanceMatrix(locations)
    }

    private def getRemoteDistance(locations: List[Location]): Future[TimeDistanceResponse] = Future {
        val points = locations.map(_.toPoint).asJava
        val req = MapboxMatrix.builder()
            .accessToken(apiKey)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .coordinates(points)

            .build()
        val ans = req.executeCall()
        //        throw ExternalApiError(ans.body().toString)
        //        if (ans.body().distances() == null) {
        //            throw ExternalApiError("Unable to get distance matrix")
        //        }
        val javaMatrix = ans.body().durations().asScala.toArray.map(x => x.map(_.doubleValue()))

        val time = javaMatrix
        val distance = javaMatrix.map(row => row.map(x => averageSpeed * x))
        TimeDistanceResponse(distance, time)
    }

    implicit class PointConverter(location: Location) {
        def toPoint: Point = Point.fromLngLat(this.location.longitude, this.location.latitude)
    }


}
