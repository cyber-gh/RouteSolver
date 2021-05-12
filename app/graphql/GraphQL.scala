package graphql

import graphql.schemas.DriversSchema
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

}
