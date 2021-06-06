package repositories.auth0

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

    override def listDrivers: Future[List[User]] = for {
        api <- getApi
        drivers <- api.roles().listUsers(driverRoleId, new PageFilter()).executeAsync().asScala.map(x => x.getItems.asScala.toList)
    } yield drivers

    private def getApi: Future[ManagementAPI] = api match {
        case Some(value) => Future.successful(value)
        case None =>
            buildMgmt().map(
                x => {
                    api = Some(x)
                    x
                }
            )
    }

    private lazy val authApi = new AuthAPI(domain, clientId, clientSecret)

    def buildMgmt(): Future[ManagementAPI] = for {
        token <- authApi.requestToken(s"https://$domain/api/v2/").executeAsync().asScala
        mgmt = new ManagementAPI(domain, token.getAccessToken)
    } yield mgmt


    //    def buildMgmt(): Future[ManagementAPI] = {
    //
    //        if (target == "dev" && token.nonEmpty) {
    //            return Future {
    //                val mgmt = new ManagementAPI(domain, token)
    //                mgmt
    //            }
    //        }
    //        return Future.failed(new Exception("Cannot obtain Auth0 Management API for production target"))
    //    }

    private def driverRoleId: String = "rol_iT25htSYLou59w6P"

    override def registerDriver(name: String, email: String): Future[User] = for {
        api <- getApi
        user <- api.users().create(buildUser(name, email)).executeAsync().asScala
        _ <- api.roles().assignUsers(driverRoleId, List(user.getId).asJava).executeAsync().asScala
    } yield user

    private def buildUser(name: String, email: String): User = {
        val user = new User(usersConnection)
        user.setName(name)
        user.setEmail(email)
        user.setPassword("Ab123456".toCharArray)
        return user
    }
}
