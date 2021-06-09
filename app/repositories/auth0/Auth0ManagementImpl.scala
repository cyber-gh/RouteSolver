package repositories.auth0

import akka.http.scaladsl.model.DateTime
import com.auth0.client.auth.AuthAPI
import com.auth0.client.mgmt.ManagementAPI
import com.auth0.client.mgmt.filter.PageFilter
import com.auth0.json.mgmt.users.User
import com.google.inject.Inject

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters._

class Auth0ManagementImpl @Inject()(implicit val executionContext: ExecutionContext) extends Auth0Management {

    private val target = scala.util.Properties.envOrElse("TARGET", "dev")

    private val domain = scala.util.Properties.envOrElse("AUTH0_DOMAIN", "")
    private val clientId = scala.util.Properties.envOrElse("AUTH0_CLIENT_ID", "")
    private val clientSecret = scala.util.Properties.envOrElse("AUTH0_CLIENT_SECRET", "")

    private val token = scala.util.Properties.envOrElse("AUTH0_MANAGEMENT_TOKEN", "")
    private val usersConnection = scala.util.Properties.envOrElse("AUTH0_USERS_CONNECTION", "")


    private var api: Option[ManagementAPI] = None

    override def ensureSupplierPermissions(userId: String): Future[Boolean] =
        for {
            api <- getApi
            _ <- api.roles().assignUsers(supplierRoleId, List(userId).asJava).executeAsync().asScala
        } yield true

    override def listDrivers: Future[List[User]] = for {
        api <- getApi
        drivers <- api.roles().listUsers(driverRoleId, new PageFilter()).executeAsync().asScala.map(x => x.getItems.asScala.toList)
    } yield drivers

    override def registerDriver(name: String, email: String): Future[User] = for {
        api <- getApi
        user <- api.users().create(buildUser(name, email)).executeAsync().asScala
        _ <- api.roles().assignUsers(driverRoleId, List(user.getId).asJava).executeAsync().asScala
    } yield user

    private var lastGenerateTokenTime: DateTime = DateTime.now

    private def getApi: Future[ManagementAPI] =
        api match {
            case Some(value) =>
                if (DateTime.now.compare(lastGenerateTokenTime) < 12 * 60 * 1000) Future.successful(value)
                else buildMgmt().map {
                    case (x, tm) => {
                        api = Some(x)
                        lastGenerateTokenTime = tm
                        x
                    }
                }
            case None =>
                buildMgmt().map {
                    case (x, tm) => {
                        api = Some(x)
                        lastGenerateTokenTime = tm
                        x
                    }
                }
        }

    private lazy val authApi = new AuthAPI(domain, clientId, clientSecret)

    def buildMgmt(): Future[(ManagementAPI, DateTime)] = for {
        token <- authApi.requestToken(s"https://$domain/api/v2/").executeAsync().asScala
        mgmt = new ManagementAPI(domain, token.getAccessToken)
        tm = DateTime.now
    } yield (mgmt, tm)

    private def driverRoleId: String = "rol_iT25htSYLou59w6P"

    private def supplierRoleId: String = "rol_Rj1uY71gQls2neEL"

    private def buildUser(name: String, email: String): User = {
        val user = new User(usersConnection)
        user.setName(name)
        user.setEmail(email)
        user.setPassword("Ab123456".toCharArray)
        return user
    }
}
