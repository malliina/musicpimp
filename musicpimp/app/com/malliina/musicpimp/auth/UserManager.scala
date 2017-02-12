package com.malliina.musicpimp.auth

import scala.concurrent.Future

trait UserManager[U, P] {

  def defaultUser: U

  def defaultPass: P

  def isDefaultCredentials = authenticate(defaultUser, defaultPass)

  /**
    *
    * @param user username
    * @param pass password
    * @return true if the credentials are valid, false otherwise
    */
  def authenticate(user: U, pass: P): Future[Boolean]

  def updatePassword(user: U, newPass: P): Future[Unit]

  def addUser(user: U, pass: P): Future[Option[AlreadyExists]]

  def deleteUser(user: U): Future[Unit]

  def users: Future[Seq[U]]

  case class AlreadyExists(user: U)

}
