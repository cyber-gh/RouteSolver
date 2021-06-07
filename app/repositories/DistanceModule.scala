package repositories

import com.google.inject.{AbstractModule, Scopes}
import repositories.solver.distance.{DistanceRepository, MapboxDistanceRepository}

class DistanceModule extends AbstractModule {
    override def configure(): Unit = {
        bind(classOf[DistanceRepository]).to(classOf[MapboxDistanceRepository]).in(Scopes.SINGLETON)
    }

}
