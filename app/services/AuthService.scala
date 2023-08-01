package services

import com.auth0.jwk.UrlJwkProvider
import com.google.inject.Inject
import pdi.jwt.{JwtAlgorithm, JwtBase64, JwtClaim, JwtJson}
import play.api.Configuration

import java.time.Clock
import scala.util.{Failure, Success, Try}

class AuthService @Inject()(config: Configuration) {
    implicit private val clock: Clock = Clock.systemUTC

    private val jwtRegex = """(.+?)\.(.+?)\.(.+?)""".r


    private val splitToken = (jwt: String) => jwt match {
        case jwtRegex(header, body, sig) => Success((header, body, sig))
        case _ => Failure(new Exception("Token does not match the correct pattern"))
    }
    // As the header and claims data are base64-encoded, this function
    // decodes those elements
    private val decodeElements = (data: Try[(String, String, String)]) => data map {
        case (header, body, sig) =>
            (JwtBase64.decodeString(header), JwtBase64.decodeString(body), sig)
    }
    // Gets the JWK from the JWKS endpoint using the jwks-rsa library
    private val getJwk = (token: String) =>
        (splitToken andThen decodeElements) (token) flatMap {
            case (header, _, _) =>
                val jwtHeader = JwtJson.parseHeader(header) // extract the header
                val jwkProvider = new UrlJwkProvider(s"$domain")

                // Use jwkProvider to load the JWKS data and return the JWK
                jwtHeader.keyId.map { k =>
                    Try(jwkProvider.get(k))
                } getOrElse Failure(new Exception("Unable to retrieve kid"))
        }
    private val validateClaims = (claims: JwtClaim) =>
        if (claims.isValid(issuer, audience)) {
            Success(claims)
        } else {
            Failure(new Exception("The JWT did not pass validation"))
        }

    def validateJwt(token: String): Try[JwtClaim] = {
        val result = for {
            jwk <- getJwk(token)
            claims <- JwtJson.decode(token, jwk.getPublicKey, Seq(JwtAlgorithm.RS256))
            _ <- validateClaims(claims)
        } yield claims

        result.recover {
            case e: Throwable => throw new Exception(s"JWT validation failed ${e.getLocalizedMessage} stacktrace -  ${e.getStackTrace.mkString("Array(", ",\n ", ")")}", e)
        }
    }

    // Your Auth0 audience, read from configuration
//    private def audience = config.get[String]("auth0.audience")
    private def audience = scala.util.Properties.envOrElse("AUTH0_AUDIENCE", "")

    private def issuer = s"https://$domain/"

    // Your Auth0 domain, read from configuration
//    private def domain = config.get[String]("auth0.domain")
        private def domain = scala.util.Properties.envOrElse("AUTH0_DOMAIN", "")
}
