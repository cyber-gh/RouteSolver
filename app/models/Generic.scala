package models


trait Identifiable {
  def id: String
}

trait AppUser extends  Identifiable {
    def name: String

    def email: String
}
