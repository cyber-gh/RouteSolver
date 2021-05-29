package repositories

import com.google.inject.{AbstractModule, Scopes}
import repositories.clients.{ClientsRepository, ClientsRepositoryImpl}

class ClientsModule extends AbstractModule {
    override def configure(): Unit = {
        bind(classOf[ClientsRepository]).to(classOf[ClientsRepositoryImpl]).in(Scopes.SINGLETON)
    }
}
