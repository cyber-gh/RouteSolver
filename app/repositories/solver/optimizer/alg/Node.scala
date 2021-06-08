package repositories.solver.optimizer.alg

import scala.collection.mutable.ListBuffer

case class Node(
                   idx: Int,
                   children: ListBuffer[Node] = ListBuffer(),
                   isRoot: Boolean = false
               ) {
    def addChild(child: Node): Unit = {
        children.addOne(child)
    }

    def traverseRoute(): List[Int] = {
        if (children.isEmpty) return List(idx)
        return children.foldLeft(List[Int]())((acc, nxt) => acc ++ nxt.traverseRoute())
    }

    def traverseOddNodes(): List[Int] = {
        if (children.isEmpty) return List(idx)
        val starter: List[Int] = if (isRoot && children.size % 2 != 0 || !isRoot && children.size % 2 == 0) List(idx) else List()
        return children.foldLeft(starter)((acc, nxt) => acc ++ nxt.traverseOddNodes())
    }
}
