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

    override def getDistanceMatrix(locations: List[Location]): Future[Array[Array[Double]]] = {
        if (locations.length <= 10) getRemoteDistance(locations)
        else directDistanceRepository.getDistanceMatrix(locations)
    }

    private def getRemoteDistance(locations: List[Location]): Future[Array[Array[Double]]] = Future {
        val points = locations.map(_.toPoint).asJava
        val req = MapboxMatrix.builder()
            .accessToken(apiKey)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .coordinates(points)
            .build()
        val ans = req.executeCall()
        val javaMatrix = ans.body().distances().asScala.toArray.map(x => x.map(_.doubleValue()))

        javaMatrix
    }

    implicit class PointConverter(location: Location) {
        def toPoint: Point = Point.fromLngLat(this.location.longitude, this.location.latitude)
    }


}
