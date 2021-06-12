package repositories.solver.optimizer.alg

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class ChristofidesAlg(
                              distanceMatrix: Array[Array[Double]],
                              nr: Int,
                              roundTrip: Boolean = false,
                              verbose: Boolean = true
                          ) {

    def solve(): (List[Int], Double) = {
        val pr = prim()
        val matching = greedyMatch(pr)
        val multiGraph = multigraph(matching, pr)

        val circuit = getEulerCircuit(multiGraph)
        val finalPath = cutPath(circuit)
        return (finalPath, cost(finalPath))
    }

    private def prim(): List[Int] = {
        val queue = mutable.Queue[Int]()
        for (x <- 0 until nr) {
            queue.addOne(x)
        }
        val isInTree: Array[Boolean] = Range(0, nr).map(x => false).toArray
        val key: Array[Double] = Range(0, nr).map(x => Double.MaxValue).toArray //distance from parent[i]
        val parent: Array[Int] = Range(0, nr).map(x => 0).toArray // parent of element i

        key(0) = 0
        var u = 0
        do {
            isInTree(u) = true;
            queue.remove(queue.indexOf(u))

            for (nxt <- 0 until nr) {
                if (!isInTree(nxt) && distanceMatrix(u)(nxt) < key(nxt)) {
                    parent(nxt) = u;
                    key(nxt) = distanceMatrix(u)(nxt)
                }
            }

            val nxt = queue.minByOption(x => key(x))
            if (nxt.nonEmpty) u = nxt.get
        } while (queue.nonEmpty)

        if (verbose) {
            println("Key vector")
            println(key.mkString("Array(", ", ", ")"))

            println("Parent vector")
            println(parent.mkString("Array(", ", ", ")"))

            println("tree cost")
            println(
                Range(0, nr).map(key(_)).sum
            )
        }

        return parent.toList
    }

    private def bestMatch(nodes: List[Int]): List[(Int, Int)] = {
        val ans = nodes.permutations.map(x => x.zip(nodes))
            .filter(x => {
                !x.exists(t => t._2 == t._1)
            }).map(x => {
            x.map(t => (scala.math.min(t._1, t._2), scala.math.max(t._1, t._2))).distinct
        }).minBy(x => {
            x.map(x => distanceMatrix(x._2)(x._1)).sum
        })

        println("Best match")
        println(ans)
        println("Matching cost")
        println(
            ans.map(x => distanceMatrix(x._1)(x._2)).sum
        )
        return ans
    }

    private def greedyMatch(minimumTree: List[Int]): List[(Int, Int)] = {
        val nodes = minimumTree.indices.map(x => Node(x, isRoot = x == 0))
        for (i <- minimumTree.indices) {
            if (minimumTree(i) != i) nodes(minimumTree(i)).addChild(nodes(i))
        }
        val oddDegreeNodes = nodes(0).traverseOddNodes()
        val nOdd = oddDegreeNodes.length
        if (verbose) {
            println("Odd Degree Nodes")
            println(oddDegreeNodes.toString)
        }

        if (nOdd < 10) {
            return bestMatch(oddDegreeNodes)
        }
        val edges: Array[Array[Edge]] = Range(0, nOdd)
            .map(x => Range(0, nOdd).map(y => {
                if (oddDegreeNodes(x) != oddDegreeNodes(y)) Edge(oddDegreeNodes(x), oddDegreeNodes(y),
                    distanceMatrix(oddDegreeNodes(x))(oddDegreeNodes(y)))
                else Edge(oddDegreeNodes(x), oddDegreeNodes(y), Double.MaxValue)
            }).sortBy(el => el.cost).toArray).toArray

        val matched = Range(0, nr).map(x => false).toArray
        val mat = Range(0, nOdd / 2).map(_ => (0, 0)).toArray
        var k = 0
        for (i <- 0 until nOdd) {
            for (j <- 0 until nOdd) {
                if (matched(edges(i)(j).from) || matched(edges(i)(j).to)) {

                } else {
                    matched(edges(i)(j).from) = true
                    matched(edges(i)(j).to) = true
                    mat(k) = (edges(i)(j).from, edges(i)(j).to)
                    k += 1
                }
            }
        }
        if (verbose) {
            println("Greedy Matching")
            for (t <- mat) {
                println(t)
            }

            println("Matching cost")
            println(
                mat.map(x => distanceMatrix(x._1)(x._2)).sum
            )

        }
        return mat.toList

    }

    private def multigraph(mat: List[(Int, Int)], mst: List[Int]): List[GraphNode] = {
        val nodes = mst.indices.map(x => GraphNode(x)).toArray

        for (i <- 1 until mst.length) {
            nodes(i).addChild(nodes(mst(i)))
            nodes(mst(i)).addChild(nodes(i))
        }

        for (i <- mat.indices) {
            nodes(mat(i)._1).addChild(nodes(mat(i)._2))
            nodes(mat(i)._1).addChild(nodes(mat(i)._1))
        }

        //        if (verbose) {
        //            println("Multigraph is ")
        //            for (node <- nodes) {
        //                println(node.idx)
        //                println(node.children.map(x => x.idx))
        //            }
        //        }
        return nodes.toList
    }

    private def cost(p: List[Int]): Double = {
        val pairs = p.sliding(2).collect { case List(a, b) => (a, b) }.toList
        return pairs.map { case (x, y) => distanceMatrix(x)(y) }.sum + (if (roundTrip) distanceMatrix(p.last)(p.head) else 0)
    }

    private def getEulerCircuit(nodes: List[GraphNode]): List[Int] = {
        val path = eulerCircuit(nodes.find(x => x.idx == 0).get).reverse

        if (verbose) {
            println("Euler circuit")
            println(path)
        }

        path
    }

    private def cutPath(path: List[Int]): List[Int] = {
        val exists = Range(0, nr).map(_ => false).toArray
        val finalPath = ListBuffer[Int]()
        for (el <- path) {
            if (!exists(el)) {
                finalPath.addOne(el)
                exists(el) = true
            }
        }

        if (verbose) {
            println("Final path")
            println(finalPath)
        }

        return finalPath.toList
    }

    def eulerCircuit(node: GraphNode): List[Int] = {
        var path = List[Int]()
        while (node.children.nonEmpty) {
            val nxt = node.children.head
            node.children.remove(0)
            nxt.removeChild(node)
            path ++= eulerCircuit(nxt)
        }
        path ++ List(node.idx)
    }
}