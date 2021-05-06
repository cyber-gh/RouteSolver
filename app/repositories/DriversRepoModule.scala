package repositories

import com.google.inject.{AbstractModule, Scopes}

class DriversRepoModule extends AbstractModule{
  override def configure(): Unit = {
    bind(classOf[DriversRepository]).to(classOf[DriversRepositoryImpl]).in(Scopes.SINGLETON)
  }
}
