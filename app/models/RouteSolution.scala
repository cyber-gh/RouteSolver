package models

import akka.http.scaladsl.model.DateTime


case class RouteSolution(
                            id: String,
                            routeId: String,

                            nrOrders: Int,
                            distance: Double, // in meters
                            time: Double, // in seconds

                            totalWeight: Option[Double],
                            totalVolume: Option[Double]

                        )

case class DeliveryOrderSolution(
                                    id: String,
                                    orderId: String,

                                    order: Int,
                                    estimatedArrivalTime: DateTime,
                                    estimatedDepartureTime: DateTime
                                )
