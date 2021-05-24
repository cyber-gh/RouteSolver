package graphql

import graphql.middleware.{AuthorizationException, UserDetails}
import graphql.schemas.DriversSchema
import sangria.execution.{HandledException, ExceptionHandler => EHandler}
import sangria.schema.{ObjectType, fields}

import javax.inject.Inject

class GraphQL @Inject()(driversSchema: DriversSchema) {

    val Schema = sangria.schema.Schema(
        query = ObjectType("Query",
            fields(driversSchema.Queries: _*)
        ),
        mutation = Some(
            ObjectType("Mutation",
                fields(driversSchema.Mutations: _*))
        )
    )

    val ErrorHandler = EHandler {
        case (_, AuthorizationException(message)) ⇒ HandledException(message)
    }
}

case class MyContext(val userDetails: UserDetails) {
    def hasPermission(permission: String): Boolean = userDetails.userPermissions.contains(permission)
}
