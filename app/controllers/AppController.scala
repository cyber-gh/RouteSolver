package controllers

import auth.AuthAction
import com.google.inject.Inject
import graphql.middleware.{AuthMiddleware, AuthorizationException, UserDetails}
import graphql.{GraphQL, MyContext}
import pdi.jwt.JwtClaim
import play.api.http.HeaderNames
import play.api.libs.json._
import play.api.mvc._
import sangria.ast.Document
import sangria.execution._
import sangria.marshalling.playJson._
import sangria.parser.QueryParser
import services.AuthService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AppController @Inject()(graphQL: GraphQL, cc: ControllerComponents, authService: AuthService, authAction: AuthAction,
                              implicit val executionContext: ExecutionContext) extends AbstractController(cc) {

    private val authEnabled: Boolean = true

    def graphiql = Action(Ok(views.html.graphiql()))

    private val headerTokenRegex = """Bearer (.+?)""".r

    def graphqlBody = Action.async(parse.json) {
        implicit request: Request[JsValue] => {
            val extract: JsValue => (String, Option[String], Option[JsObject]) = query => (
                (query \ "query").as[String],
                (query \ "operationName").asOpt[String],
                (query \ "variables").toOption.flatMap {
                    case JsString(vars) => Some(parseVariables(vars))
                    case obj: JsObject => Some(obj)
                    case _ => None
                }
            )

            val maybeQuery: Try[(String, Option[String], Option[JsObject])] = Try {
                request.body match {
                    case arrayBody@JsArray(_) => extract(arrayBody.value(0))
                    case objectBody@JsObject(_) => extract(objectBody)
                    case otherType =>
                        throw new Error {
                            s"/graphql endpoint does not support request body of type [${otherType.getClass.getSimpleName}]"
                        }
                }
            }

            extractBearerToken(request) map { token =>
                authService.validateJwt(token) match {
                    case Success(claim) =>
                        maybeQuery match {
                            case Success((query, operationName, variables)) => executeQuery(query, variables, operationName, UserDetails(claim.subject.getOrElse(""), permissions(claim)))
                            case Failure(error) => Future.successful {
                                BadRequest(error.getMessage)
                            }
                        }
                    case Failure(t) => Future.successful(Results.Unauthorized(t.getMessage)) // token was invalid - return 401
                }
            } getOrElse Future.successful(Results.Unauthorized)


        }
    }

    private def extractBearerToken[A](request: Request[A]): Option[String] =
        request.headers.get(HeaderNames.AUTHORIZATION) collect {
            case headerTokenRegex(token) => token
        }


    private def parseVariables(variables: String): JsObject = if (variables.trim.isEmpty || variables.trim == "null") Json.obj()
    else Json.parse(variables).as[JsObject]

    private def executeQuery(query: String, variables: Option[JsObject] = None, operation: Option[String] = None, userDetails: UserDetails): Future[Result] = QueryParser.parse(query) match {
        case Success(queryAst: Document) => Executor.execute(
            schema = graphQL.Schema,
            queryAst = queryAst,
            variables = variables.getOrElse(Json.obj()),
            operationName = operation,
            userContext = MyContext(userDetails),
            middleware = AuthMiddleware :: Nil,
            exceptionHandler = graphQL.ErrorHandler
        )
            .map(x => if (hasErrors(x)) Results.Unauthorized(x) else Ok(x)).recover {
            case error: QueryAnalysisError => BadRequest(error.resolveError)
            case error: AuthorizationException => BadRequest(error.message)
            case error: ErrorWithResolver => InternalServerError(error.resolveError)
        }
        case Failure(exception) => Future(BadRequest(s"${exception.getMessage}"))
    }

    private def hasErrors(js: JsValue): Boolean = {
        val t = js \ "errors" \ 0 \ "message"
        val msg = t.asOpt[String].getOrElse("")
        return msg == "You don't have the required permissions"
    }

    private def permissions(claim: JwtClaim): List[String] = (Json.parse(claim.content) \ "permissions").asOpt[List[String]].getOrElse(List())

    def ping = authAction { implicit request =>
        Ok(request.jwt.content)
    }

}
