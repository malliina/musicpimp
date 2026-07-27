package com.malliina.musicpimp.auth

trait UserManager[F[_], U, P]:
  def defaultUser: U
  def defaultPass: P
  def isDefaultCredentials = authenticate(defaultUser, defaultPass)

  /** @param user
    *   username
    * @param pass
    *   password
    * @return
    *   true if the credentials are valid, false otherwise
    */
  def authenticate(user: U, pass: P): F[Boolean]
  def updatePassword(user: U, newPass: P): F[Unit]
  def addUser(user: U, pass: P): F[Option[AlreadyExists]]
  def deleteUser(user: U): F[Unit]
  def users: F[List[U]]
  case class AlreadyExists(user: U)
