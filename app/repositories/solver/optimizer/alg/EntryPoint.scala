package repositories.solver.optimizer.alg

import scala.io.Source

object EntryPoint {

    def main(args: Array[String]): Unit = {
        val fileName = "src/main/scala/datasets/input4.txt"
        val matrix: Array[Array[Double]] = Source.fromFile(fileName).getLines().map(x => x.split(" ").map(t => t.toDouble)).toArray

        val alg = ChristofidesAlg(matrix, matrix.length, roundTrip = true, verbose = true)

        val sol = alg.solve()
        println(sol._2)

    }
}
