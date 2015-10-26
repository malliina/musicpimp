package com.mle.musicpimp.auth

import scala.concurrent.Future

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
  def authenticate(user: User, pass: Password): Future[Boolean]

  def updatePassword(user: User, newPass: Password): Future[Unit]

  def addUser(user: User, pass: Password): Future[Option[AlreadyExists]]

  def deleteUser(user: User): Future[Unit]

  def users: Future[Seq[User]]

  case class AlreadyExists(user: User)

}
