package repositories.auth0

import com.auth0.json.mgmt.users.User

import scala.concurrent.Future

trait Auth0Management {
    def listDrivers: Future[List[User]]
    def registerDriver(name: String, email: String): Future[User]
    def ensureSupplierPermissions(userId: String): Future[Boolean]
}
