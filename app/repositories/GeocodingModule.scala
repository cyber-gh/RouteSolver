package repositories

import com.google.inject.{AbstractModule, Scopes}
import repositories.geocoding.{GeocodingRepository, GoogleGeocodingRepository}

class GeocodingModule extends AbstractModule {
    override def configure(): Unit = {
        bind(classOf[GeocodingRepository]).to(classOf[GoogleGeocodingRepository]).in(Scopes.SINGLETON)
    }
}
