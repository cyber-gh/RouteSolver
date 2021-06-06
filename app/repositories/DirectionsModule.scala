package repositories

import com.google.inject.{AbstractModule, Scopes}
import repositories.directions.{DirectionsRepository, MapboxDirectionsRepositoryImpl}

class DirectionsModule extends AbstractModule {
    override def configure(): Unit = {
        bind(classOf[DirectionsRepository]).to(classOf[MapboxDirectionsRepositoryImpl]).in(Scopes.SINGLETON)
    }

}
