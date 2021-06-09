package graphql.middleware

import graphql.MyContext
import sangria.execution._
import sangria.schema.Context

object AuthMiddleware extends Middleware[MyContext] with MiddlewareBeforeField[MyContext] {

    override type QueryVal = Unit
    override type FieldVal = Unit

    override def beforeQuery(context: MiddlewareQueryContext[MyContext, _, _]) = ()

    override def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[MyContext, _, _]) = ()

    override def beforeField(queryVal: Unit, mctx: MiddlewareQueryContext[MyContext, _, _], ctx: Context[MyContext, _]): BeforeFieldResult[MyContext, Unit] = {
        val maybePermission = ctx.field.tags.collectFirst {
            case x if x.isInstanceOf[AuthPermission] => x.asInstanceOf[AuthPermission].permissionName
        }

        continue
        //        maybePermission match {
        //            case Some(value) => {
        //                if (!ctx.ctx.hasPermission(value)) throw AuthorizationException("You don't have the required permissions")
        //                else continue
        //            }
        //            case None => continue
        //        }
    }
}

case class AuthenticationException(message: String) extends Exception(message)

case class AuthorizationException(message: String) extends Exception(message) with UserFacingError


case class UserDetails(userId: String, userPermissions: List[String])

case class AuthPermission(val permissionName: String) extends FieldTag