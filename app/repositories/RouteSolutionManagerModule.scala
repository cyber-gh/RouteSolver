package repositories

import com.google.inject.{AbstractModule, Scopes}
import repositories.solver.{RouteSolutionManager, RouteSolutionManagerImpl}

class RouteSolutionManagerModule extends AbstractModule {
    override def configure(): Unit = {
        bind(classOf[RouteSolutionManager]).to(classOf[RouteSolutionManagerImpl]).in(Scopes.SINGLETON)
    }

}
