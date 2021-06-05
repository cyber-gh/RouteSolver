package graphql.schemas

import akka.http.scaladsl.model.DateTime
import com.google.inject.Inject
import graphql.MyContext
import graphql.middleware.AuthPermission
import graphql.resolvers.{ClientsResolver, DeliveryResolver, DriversResolver}
import models._
import sangria.ast.StringValue
import sangria.macros.derive._
import sangria.schema.{ObjectType, _}
import spray.json.DefaultJsonProtocol._

class DriversSchema @Inject()(
                                 driversResolver: DriversResolver,
                                 clientsResolver: ClientsResolver,
                                 deliveryResolver: DeliveryResolver) {

    implicit val GraphQLDateTime = ScalarType[DateTime]( //1
        "DateTime", //2
        coerceOutput = (dt, _) => dt.toString, //3
        coerceInput = { //4
            case StringValue(dt, _, _, _, _) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
            case _ => Left(DateTimeCoerceViolation)
        },
        coerceUserInput = { //5
            case s: String => DateTime.fromIsoDateTimeString(s).toRight(DateTimeCoerceViolation)
            case _ => Left(DateTimeCoerceViolation)
        }
    )

    implicit lazy val RouteStateType = deriveEnumType[RouteState.Value]()

    implicit lazy val DriverType: ObjectType[Unit, Driver] = deriveObjectType[Unit, Driver](
        Interfaces(IdentifiableType),
        ObjectTypeName("Driver"),
        ExcludeFields("supplierId")
    )
    implicit lazy val DeliveryClientType: ObjectType[Unit, DeliveryClient] = deriveObjectType[Unit, DeliveryClient](
        Interfaces(IdentifiableType),
        ReplaceField("locationId",
            Field(name = "location", LocationType, resolve = it => deliveryResolver.getLocation(it.value.locationId))
        ),
        ExcludeFields("supplierId")
    )

    implicit lazy val LocationType: ObjectType[Unit, Location] = deriveObjectType[Unit, Location](ObjectTypeName("Location"))
    implicit lazy val DeliveryOrderType: ObjectType[Unit, DeliveryOrderModel] = deriveObjectType[Unit, DeliveryOrderModel](
        Interfaces(IdentifiableType),
        ReplaceField("locationId",
            Field("location", LocationType, resolve = it => deliveryResolver.getLocation(it.value.locationId))
        ),
        ReplaceField("routeId",
            Field("route", OptionType(DeliveryRouteType), resolve = it => deliveryResolver.getRoute(it.value.routeId))
        )
    )
    implicit lazy val DeliveryRouteType: ObjectType[Unit, DeliveryRouteModel] = deriveObjectType[Unit, DeliveryRouteModel](
        Interfaces(IdentifiableType),
        ObjectTypeName("Route"),
        AddFields(
            Field("orders", ListType(DeliveryOrderType),
                resolve = c => deliveryResolver.getOrders(c.value.id))
        ),
        ReplaceField("startLocationId",
            Field("startLocation", LocationType, resolve = it => deliveryResolver.getLocation(it.value.startLocationId))
        ),
        ReplaceField("startTime",
            Field("startTime", GraphQLDateTime, resolve = _.value.startTime)
        ),
        ExcludeFields("supplierId")
    )

    implicit val locationFormat = jsonFormat3(Location)
    implicit val LocationInputType: InputObjectType[Location] = deriveInputObjectType[Location]()
    private val IdentifiableType = InterfaceType(
        "Identifiable",
        fields[Unit, Identifiable](
            Field("id", StringType, resolve = _.value.id)
        )
    )
    private val Id = Argument("id", StringType)
    private val DriversMutations: List[Field[MyContext, Unit]] = List(
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
    private val ClientsMutations: List[Field[MyContext, Unit]] = List(
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
    private val DeliveryMutations: List[Field[MyContext, Unit]] = List(
        Field(
            name = "addRoute",
            fieldType = DeliveryRouteType,
            tags = AuthPermission("modify:routes") :: Nil,
            arguments = List(
                Argument("name", StringType),
                Argument("startAddress", StringType),
                Argument("roundTrip", BooleanType)
            ),
            resolve = ctx => deliveryResolver.addRoute(
                ctx.ctx.userDetails.userId,
                ctx.args.arg[String]("name"),
                ctx.args.arg[String]("startAddress"),
                ctx.args.arg[Boolean]("roundTrip")
            )
        ),

        Field(
            name = "deleteRoute",
            fieldType = BooleanType,
            tags = AuthPermission("modify:routes") :: Nil,
            arguments = List(Argument("routeId", StringType)),
            resolve = ctx => deliveryResolver.deleteRoute(ctx.args.arg[String]("routeId"))
        ),
        Field(
            name = "addOrder",
            fieldType = DeliveryOrderType,
            tags = AuthPermission("modify:routes") :: Nil,
            arguments = List(
                Argument("routeId", StringType),
                Argument("address", StringType)
            ),
            resolve = ctx => deliveryResolver.addOrder(
                ctx.args.arg[String]("routeId"),
                ctx.args.arg[String]("address")
            )
        ),
        Field(
            name = "deleteOrder",
            fieldType = BooleanType,
            tags = AuthPermission("modify:routes") :: Nil,
            arguments = List(Argument("orderId", StringType)),
            resolve = ctx => deliveryResolver.deleteOrder(
                ctx.args.arg[String]("orderId")
            )
        )
    )
    private val DeliveryQueries: List[Field[MyContext, Unit]] = List(
        Field(
            name = "routes",
            fieldType = ListType(DeliveryRouteType),
            tags = AuthPermission("read:routes") :: Nil,
            resolve = ctx => deliveryResolver.getRoutes(ctx.ctx.userDetails.userId)
        ),
        Field(
            name = "findRoute",
            fieldType = OptionType(DeliveryRouteType),
            tags = AuthPermission("read:routes") :: Nil,
            arguments = Id :: Nil,
            resolve = ctx => deliveryResolver.getRoute(ctx.arg(Id))
        )
    )


    val Queries: List[Field[MyContext, Unit]] = ClientsQueries ++ DriversQueries ++ DeliveryQueries
    val Mutations: List[Field[MyContext, Unit]] = DriversMutations ++ ClientsMutations ++ DeliveryMutations

}
