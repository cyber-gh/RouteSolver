package graphql.schemas

import com.google.inject.Inject
import graphql.MyContext
import graphql.middleware.AuthPermission
import graphql.resolvers.DriversResolver
import models.Driver
import sangria.macros.derive.{ObjectTypeName, deriveObjectType}
import sangria.schema.{ObjectType, _}

class DriversSchema @Inject()(driversResolver: DriversResolver) {

    implicit val DriverType: ObjectType[Unit, Driver] = deriveObjectType[Unit, Driver](ObjectTypeName("Driver"))


    val Queries: List[Field[MyContext, Unit]] = List(
        Field(
            name = "drivers",
            fieldType = ListType(DriverType),
            tags = AuthPermission("read:drivers") :: Nil,
            resolve = _ => driversResolver.drivers
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

    val Mutations: List[Field[MyContext, Unit]] = List(
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
                    ctx.args.arg[String]("email")
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
}
