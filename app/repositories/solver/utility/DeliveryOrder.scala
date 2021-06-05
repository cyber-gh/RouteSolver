package repositories.solver.utility

import models.{Identifiable, Location}

case class DeliveryOrder(
                            id: String,
                            routeId: String,

                            location: Location,
                            startTime: Option[String],
                            endTime: Option[String],

                            weight: Option[Double],
                            volume: Option[Double]
                        ) extends Identifiable
