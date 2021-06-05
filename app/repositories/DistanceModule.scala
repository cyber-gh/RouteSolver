package repositories

import com.google.inject.{AbstractModule, Scopes}
import repositories.solver.distance.{DirectDistanceRepository, DistanceRepository}

class DistanceModule extends AbstractModule {
    override def configure(): Unit = {
        bind(classOf[DistanceRepository]).to(classOf[DirectDistanceRepository]).in(Scopes.SINGLETON)
    }

}
