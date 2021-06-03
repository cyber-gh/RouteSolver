package models

import sangria.validation.Violation


trait Identifiable {
    def id: String
}

trait AppUser extends Identifiable {
    def name: String

    def email: String
}

case object DateTimeCoerceViolation extends Violation {
    override def errorMessage: String = "Error during parsing DateTime"
}
