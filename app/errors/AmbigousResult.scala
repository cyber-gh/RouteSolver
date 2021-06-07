package errors

import sangria.execution.UserFacingError

case class AmbigousResult(msg: String) extends Exception with UserFacingError {
    override def getMessage: String = msg
}

case class UndefinedAlgorithm(msg: String) extends Exception with UserFacingError {
    override def getMessage: String = msg
}

case class EntityNotFound(msg: String) extends Exception with UserFacingError {
    override def getMessage: String = msg
}

case class OperationNotPermitted(msg: String) extends Exception with UserFacingError {
    override def getMessage: String = msg
}