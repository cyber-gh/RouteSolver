package models

case class DeliveryClientInputForm(
                                      name: String,
                                      email: String,
                                      address: String,

                                      startTime: Option[String],
                                      endTime: Option[String],

                                      weight: Option[Double],
                                      volume: Option[Double])

case class DeliveryOrderInputForm(
                                     routeId: String,
                                     name: String,
                                     address: String,
                                     clientId: Option[String],

                                     startTime: Option[String],
                                     endTime: Option[String],

                                     weight: Option[Double],
                                     volume: Option[Double]
                                 )