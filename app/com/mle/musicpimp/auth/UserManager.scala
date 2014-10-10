package com.mle.musicpimp.auth

/**
 * @author Michael
 */
trait UserManager {
  type User = String
  type Password = String

  def defaultUser: User

  def defaultPass: Password

  def isDefaultCredentials = authenticate(defaultUser, defaultPass)

  /**
   *
   * @param user username
   * @param pass password
   * @return true if the credentials are valid, false otherwise
   */
  def authenticate(user: User, pass: Password): Boolean

  def updatePassword(user: User, newPass: Password)

  def addUser(user: User, pass: Password): Option[AlreadyExists]

  def deleteUser(user: User)

  def users: Seq[User]

  case class AlreadyExists(user: User)

}
