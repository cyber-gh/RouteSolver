package repositories.solver.optimizer.alg

import scala.collection.mutable.ListBuffer

case class GraphNode(
                        idx: Int,
                        children: ListBuffer[GraphNode] = ListBuffer(),
                        var visited: Boolean = false) {

    def addChild(node: GraphNode): Unit = {
        children.addOne(node)
    }

    def removeChild(child: GraphNode): Unit = {
        val idx = children.indexOf(child)
        if (idx != -1) children.remove(children.indexOf(child))
    }

    def getNextChild(target: Int, firstTime: Boolean): List[Int] = {
        if (idx == target && !firstTime) return List(idx)
        if (children.nonEmpty) {
            val n = children.remove(0)
            n.removeChild(this)
            return List(this.idx) ++ n.getNextChild(target, firstTime = false)
        } else {
            return List()
        }
    }

    def traverseChildren(goal: Int, path: ListBuffer[Int], firstTime: Boolean): Unit = {
        if (idx == goal && !firstTime) {
            path.addOne(idx)
        } else {
            if (children.nonEmpty) {
                val tmp = children.remove(0)
                tmp.removeChild(this)
                path.addOne(idx)
                tmp.traverseChildren(goal, path, firstTime = false)
            }
        }
    }


}