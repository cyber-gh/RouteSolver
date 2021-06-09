package repositories

import com.google.inject.{AbstractModule, Scopes}
import repositories.locations.{CachedLocationsRepository, LocationRepository}
import repositories.routes.{DeliveryRouteRepository, DeliveryRouteRepositoryImpl}

class DeliveryModule extends AbstractModule {
    override def configure(): Unit = {
        bind(classOf[DeliveryRouteRepository]).to(classOf[DeliveryRouteRepositoryImpl]).in(Scopes.SINGLETON)
        bind(classOf[LocationRepository]).to(classOf[CachedLocationsRepository]).in(Scopes.SINGLETON)
    }
}
