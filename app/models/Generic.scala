package models


trait Identifiable {
  def id: String
}

trait AppUser extends  Identifiable {
  def firstName: String
  def lastName: String
  def email: String
}
