package repositories

import com.google.inject.{AbstractModule, Scopes}
import repositories.solver.optimizer.{BasicRouteOptimizer, RouteOptimizer}

class RouteOptimizerModule extends AbstractModule {
    override def configure(): Unit = {
        bind(classOf[RouteOptimizer]).to(classOf[BasicRouteOptimizer]).in(Scopes.SINGLETON)
    }

}
