package repositories.solver.utility

case class OptimizeSolution(
                               orders: List[(DeliveryOrder, Int)],
                               cost: Double
                           )
