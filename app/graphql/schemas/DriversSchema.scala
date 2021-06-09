package graphql.schemas

import akka.http.scaladsl.model.DateTime
import com.google.inject.Inject
import graphql.MyContext
import graphql.middleware.AuthPermission
import graphql.resolvers.{DeliveryResolver, DeliverySolutionResolver, DriversResolver}
import models.VRPAlg.VRPAlg
import models._
import repositories.auth0.Auth0Management
import repositories.clients.ClientsRepository
import sangria.ast.StringValue
import sangria.macros.derive._
import sangria.marshalling.sprayJson.sprayJsonReaderFromInput
import sangria.schema.{ObjectType, _}
import spray.json.DefaultJsonProtocol._

class DriversSchema @Inject()(
                                 driversResolver: DriversResolver,
                                 clientsResolver: ClientsRepository,
                                 deliveryResolver: DeliveryResolver,
                                 deliverySolutionResolver: DeliverySolutionResolver,
                                 permissionManager: Auth0Management) {


    implicit val clientFormat = jsonFormat7(DeliveryClientInputForm)
    val DeliveryClientInputType: InputObjectType[DeliveryClientInputForm] = deriveInputObjectType[DeliveryClientInputForm]()
    val DeliveryClientArg = Argument("client", DeliveryClientInputType)

    implicit val orderInputFormat = jsonFormat8(DeliveryOrderInputForm)
    val DeliveryOrderInputType = deriveInputObjectType[DeliveryOrderInputForm]()
    val DeliveryOrderInputArg = Argument("order", DeliveryOrderInputType)

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
    implicit lazy val VRPAlgType = deriveEnumType[VRPAlg.Value]()

    implicit lazy val DriverType: ObjectType[Unit, Driver] = deriveObjectType[Unit, Driver](
        Interfaces(IdentifiableType),
        ObjectTypeName("Driver"),
        ExcludeFields("supplierId", "vehicleId"),
        ReplaceField("locationId",
            Field("location", OptionType(LocationType), resolve = x => deliveryResolver.getLocation(x.value.locationId))
        )
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
        ExcludeFields("routeId", "clientId")
    )
    implicit lazy val DeliveryRouteType: ObjectType[Unit, DeliveryRouteModel] = deriveObjectType[Unit, DeliveryRouteModel](
        Interfaces(IdentifiableType),
        ObjectTypeName("Route"),
        AddFields(
            Field("orders", ListType(DeliveryOrderType),
                resolve = c => deliveryResolver.getOrders(c.value.id)),
            Field("solutions", ListType(RouteSolutionType),
                resolve = c => deliverySolutionResolver.getSolutions(c.value.id))
        ),
        ReplaceField("startLocationId",
            Field("startLocation", LocationType, resolve = it => deliveryResolver.getLocation(it.value.startLocationId))
        ),
        ReplaceField("selectedSolutionId",
            Field("selectedSolution", OptionType(RouteSolutionType), resolve = it => deliverySolutionResolver.getSolution(it.value.selectedSolutionId))
        ),
        ReplaceField("startTime",
            Field("startTime", GraphQLDateTime, resolve = _.value.startTime)
        ),
        ExcludeFields("supplierId")
    )

    implicit lazy val DeliveryOrderSolutionType: ObjectType[Unit, DeliveryOrderSolution] = deriveObjectType(
        Interfaces(IdentifiableType),
        ExcludeFields("solutionId"),
        AddFields(
            Field("details", OptionType(DeliveryOrderType), resolve = x => deliveryResolver.getOrder(x.value.orderId))
        )

    )

    implicit lazy val RouteDirectionsType: ObjectType[Unit, RouteDirections] = deriveObjectType()

    implicit lazy val RouteSolutionType: ObjectType[Unit, RouteSolution] = deriveObjectType[Unit, RouteSolution](
        Interfaces(IdentifiableType),
        ExcludeFields("routeId"),
        AddFields(
            Field("orders", ListType(DeliveryOrderSolutionType), resolve = x => deliverySolutionResolver.getSolutionDetails(x.value.id))
        ),
        ReplaceField("directionsId",
            Field("directions", OptionType(RouteDirectionsType), resolve = x => deliverySolutionResolver.getDirections(x.value.directionsId))
        )

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
                Argument("email", StringType),
                Argument("address", StringType)
            ),
            tags = AuthPermission("modify:drivers") :: Nil,
            resolve =
                ctx => driversResolver.addDriver(
                    ctx.args.arg[String]("name"),
                    ctx.args.arg[String]("email"),
                    ctx.args.arg[String]("address"),
                    ctx.ctx.userDetails.userId
                )
        ),

        Field(
            name = "updateDriverLocation",
            fieldType = BooleanType,
            arguments = List(
                Argument("lat", FloatType),
                Argument("lng", FloatType)
            ),
            resolve = ctx => driversResolver.updateLocation(ctx.ctx.userDetails.userId,
                ctx.args.arg[Float]("lat").toDouble,
                ctx.args.arg[Float]("lng").toDouble
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
            arguments = DeliveryClientArg :: Nil,
            resolve = ctx => clientsResolver.create(
                ctx.arg(DeliveryClientArg).name,
                ctx.arg(DeliveryClientArg).email,
                ctx.arg(DeliveryClientArg).address,
                ctx.ctx.userDetails.userId,
                ctx.arg(DeliveryClientArg).startTime,
                ctx.arg(DeliveryClientArg).endTime,
                ctx.arg(DeliveryClientArg).weight,
                ctx.arg(DeliveryClientArg).volume
            )
        ),
        Field(
            name = "removeClient",
            fieldType = BooleanType,
            tags = AuthPermission("modify:clients") :: Nil,
            arguments = List(
                Argument("id", StringType)
            ),
            resolve = ctx => clientsResolver.delete(
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
                Argument("address", StringType),
                Argument("name", StringType)
            ),
            resolve = ctx => deliveryResolver.addOrder(
                ctx.args.arg[String]("routeId"),
                ctx.args.arg[String]("address"),
                ctx.args.arg[String]("name")
            )
        ),

        Field(
            name = "addDetailedOrder",
            fieldType = DeliveryOrderType,
            tags = AuthPermission("modify:routes") :: Nil,
            arguments = DeliveryOrderInputArg :: Nil,
            resolve = ctx => deliveryResolver.addOrder(ctx.arg(DeliveryOrderInputArg))
        ),

        Field(
            name = "addOrderByClient",
            fieldType = ListType(DeliveryOrderType),
            tags = AuthPermission("modify:routes") :: Nil,
            arguments = List(
                Argument("routeId", StringType),
                Argument("clientIds", ListInputType(StringType))
            ),
            resolve = ctx => deliveryResolver.addOrdersByClients(
                ctx.args.arg[String]("routeId"),
                ctx.args.arg[List[String]]("clientIds")
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
        ),
        Field(
            name = "assignRouteDriver",
            fieldType = BooleanType,
            arguments = List(
                Argument("routeId", StringType),
                Argument("driverId", StringType)
            ),
            resolve = ctx => deliveryResolver.assignDriverToRoute(
                ctx.args.arg[String]("routeId"),
                ctx.args.arg[String]("driverId")
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
        ),
        Field(
            name = "routesByDriver",
            fieldType = ListType(DeliveryRouteType),
            resolve = ctx => deliveryResolver.getDriverAssignedRoutes(ctx.ctx.userDetails.userId)
        )
    )
    private val DeliverySolutionQueries: List[Field[MyContext, Unit]] = List(
        Field(
            name = "routeSolutions",
            fieldType = ListType(RouteSolutionType),
            tags = AuthPermission("read:routes") :: Nil,
            arguments = Id :: Nil,
            resolve =
                ctx => deliverySolutionResolver.getSolutions(ctx.arg(Id))
        )
    )

    private val OtherMutations: List[Field[MyContext, Unit]] = List(
        Field(
            name = "addSupplierPermissions",
            fieldType = BooleanType,
            resolve = ctx => permissionManager.ensureSupplierPermissions(ctx.ctx.userDetails.userId)
        )
    )

    private val DeliverySolutionMutations: List[Field[MyContext, Unit]] = List(
        Field(
            name = "solveRoute",
            fieldType = RouteSolutionType,
            tags = AuthPermission("modify:routes") :: Nil,
            arguments = List(
                Argument("routeId", StringType),
                Argument("algorithm", VRPAlgType)
            ),
            resolve = ctx => deliverySolutionResolver.solveRoute(
                ctx.args.arg[String]("routeId"),
                ctx.args.arg[VRPAlg]("algorithm")
            )

        ),
        Field(
            name = "setSolution",
            fieldType = OptionType(RouteSolutionType),
            tags = AuthPermission("modify:routes") :: Nil,
            arguments = List(
                Argument("routeId", StringType),
                Argument("solutionId", StringType)
            ),
            resolve =
                ctx => deliverySolutionResolver.setRouteSolution(
                    ctx.args.arg[String]("routeId"),
                    ctx.args.arg[String]("solutionId")
                )
        ),

        Field(
            name = "setBestSolution",
            fieldType = OptionType(RouteSolutionType),
            tags = AuthPermission("modify:routes") :: Nil,
            arguments = List(
                Argument("routeId", StringType)
            ),
            resolve =
                ctx => deliverySolutionResolver.setBestSolution(
                    ctx.args.arg[String]("routeId")
                )
        ),

        Field(
            name = "deleteSolution",
            fieldType = BooleanType,
            tags = AuthPermission("modify:routes") :: Nil,
            arguments = Id :: Nil,
            resolve = ctx => deliverySolutionResolver.deleteSolution(
                ctx.arg(Id)
            )

        )
    )

    val Queries: List[Field[MyContext, Unit]] = ClientsQueries ++ DriversQueries ++ DeliveryQueries ++ DeliverySolutionQueries
    val Mutations: List[Field[MyContext, Unit]] = DriversMutations ++ ClientsMutations ++ DeliveryMutations ++ DeliverySolutionMutations ++ OtherMutations

}
