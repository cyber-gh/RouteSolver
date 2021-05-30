package graphql.schemas

import com.google.inject.Inject
import graphql.MyContext
import graphql.middleware.AuthPermission
import graphql.resolvers.{ClientsResolver, DriversResolver}
import models.{DeliveryClient, Driver}
import sangria.macros.derive.{ObjectTypeName, deriveObjectType}
import sangria.schema.{ObjectType, _}

class DriversSchema @Inject()(driversResolver: DriversResolver, clientsResolver: ClientsResolver) {

    implicit val DriverType: ObjectType[Unit, Driver] = deriveObjectType[Unit, Driver](ObjectTypeName("Driver"))
    implicit val DeliveryClientType: ObjectType[Unit, DeliveryClient] = deriveObjectType[Unit, DeliveryClient](ObjectTypeName("DeliveryClient"))

    val DriversMutations: List[Field[MyContext, Unit]] = List(
        Field(
            name = "addDriver",
            fieldType = DriverType,
            arguments = List(
                Argument("name", StringType),
                Argument("email", StringType)
            ),
            tags = AuthPermission("modify:drivers") :: Nil,
            resolve =
                ctx => driversResolver.addDriver(
                    ctx.args.arg[String]("name"),
                    ctx.args.arg[String]("email"),
                    ctx.ctx.userDetails.userId
                )
        ),
        Field(
            name = "deleteDriver",
            fieldType = BooleanType,
            tags = AuthPermission("modify:drivers") :: Nil,
            arguments = List(
                Argument("id", StringType)
            ),
            resolve =
                ctx => driversResolver.deleteDriver(ctx.args.arg[String]("id"))
        )
    )
    val ClientsMutations: List[Field[MyContext, Unit]] = List(
        Field(
            name = "addClient",
            fieldType = DeliveryClientType,
            tags = AuthPermission("modify:clients") :: Nil,
            arguments = List(
                Argument("name", StringType),
                Argument("email", StringType),
                Argument("address", StringType)
            ),
            resolve = ctx => clientsResolver.addClient(
                ctx.args.arg[String]("name"),
                ctx.args.arg[String]("email"),
                ctx.args.arg[String]("address"),
                ctx.ctx.userDetails.userId
            )
        ),
        Field(
            name = "removeClient",
            fieldType = BooleanType,
            tags = AuthPermission("modify:clients") :: Nil,
            arguments = List(
                Argument("id", StringType)
            ),
            resolve = ctx => clientsResolver.removeClient(
                ctx.args.arg[String]("id")
            )
        )
    )

    private val ClientsQueries: List[Field[MyContext, Unit]] = List(
        Field(
            name = "clients",
            fieldType = ListType(DeliveryClientType),
            tags = AuthPermission("read:clients") :: Nil,
            resolve = ctx => clientsResolver.getClients(ctx.ctx.userDetails.userId)
        )
    )
    private val DriversQueries: List[Field[MyContext, Unit]] = List(
        Field(
            name = "drivers",
            fieldType = ListType(DriverType),
            tags = AuthPermission("read:drivers") :: Nil,
            resolve = ctx => driversResolver.drivers(ctx.ctx.userDetails.userId)
        ),
        Field(
            name = "findDriver",
            fieldType = OptionType(DriverType),
            tags = AuthPermission("read:drivers") :: Nil,
            arguments = List(Argument("id", StringType)),
            resolve =
                sangriaContext => driversResolver.findDriver(sangriaContext.args.arg[String]("id"))
        )
    )

    val Queries: List[Field[MyContext, Unit]] = ClientsQueries ++ DriversQueries
    val Mutations: List[Field[MyContext, Unit]] = DriversMutations ++ ClientsMutations
}
