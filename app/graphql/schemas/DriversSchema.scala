package graphql.schemas

import com.google.inject.Inject
import graphql.resolvers.DriversResolver
import models.Driver
import sangria.macros.derive.{ObjectTypeName, deriveObjectType}
import sangria.schema.{ObjectType, _}

class DriversSchema @Inject()(driversResolver: DriversResolver) {

    implicit val DriverType: ObjectType[Unit, Driver] = deriveObjectType[Unit, Driver](ObjectTypeName("Driver"))


    val Queries: List[Field[Unit, Unit]] = List(
        Field(
            name = "drivers",
            fieldType = ListType(DriverType),
            resolve = _ => driversResolver.drivers
        ),
        Field(
            name = "findDriver",
            fieldType = OptionType(DriverType),
            arguments = List(Argument("id", StringType)),
            resolve =
                sangriaContext => driversResolver.findDriver(sangriaContext.args.arg[String]("id"))
        )
    )

    val Mutations: List[Field[Unit, Unit]] = List(
        Field(
            name = "addDriver",
            fieldType = DriverType,
            arguments = List(
                Argument("firstName", StringType),
                Argument("lastName", StringType),
                Argument("email", StringType)
            ),
            resolve =
                ctx => driversResolver.addDriver(
                    ctx.args.arg[String]("firstName"),
                    ctx.args.arg[String]("lastName"),
                    ctx.args.arg[String]("email")
                )
        ),

        Field(
            name = "updateDriver",
            fieldType = DriverType,
            arguments = List(
                Argument("id", StringType),
                Argument("firstName", StringType),
                Argument("lastName", StringType),
                Argument("email", StringType)
            ),
            resolve =
                ctx => driversResolver.updateDriver(
                    ctx.args.arg[String]("id"),
                    ctx.args.arg[String]("firstName"),
                    ctx.args.arg[String]("lastName"),
                    ctx.args.arg[String]("email")
                )
        ),

        Field(
            name = "deleteDriver",
            fieldType = BooleanType,
            arguments = List(
                Argument("id", StringType)
            ),
            resolve =
                ctx => driversResolver.deleteDriver(ctx.args.arg[String]("id"))
        )
    )
}
