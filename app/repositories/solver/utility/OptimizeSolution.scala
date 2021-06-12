package repositories.solver.utility

case class OptimizeSolution(
                               orders: List[(DeliveryOrder, Int)],
                               distance: Double,
                               time: Double = 0
                           )
