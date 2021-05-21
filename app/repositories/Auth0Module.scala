package repositories

import com.google.inject.{AbstractModule, Scopes}
import repositories.auth0.{Auth0Management, Auth0ManagementImpl}

class Auth0Module extends AbstractModule {
    override def configure(): Unit = {
        bind(classOf[Auth0Management]).to(classOf[Auth0ManagementImpl]).in(Scopes.SINGLETON)
    }
}
