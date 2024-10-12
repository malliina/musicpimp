package com.malliina.musicpimp.db

import com.malliina.musicpimp.auth.{Auth, DataUser, UserManager}
import com.malliina.values.{Password, Username}
import io.getquill.*

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object NewUserManager:
  val defaultUser = Username("admin")
  val defaultPass = Password("test")

  def withUser(db: PimpMySQL): NewUserManager =
    val users = apply(db)
    Await.result(users.ensureAtLeastOneUserExists(), 10.seconds)
    users

  def apply(db: PimpMySQL): NewUserManager = new NewUserManager(db)

class NewUserManager(val db: PimpMySQL) extends UserManager[Username, Password]:
  import db.*

  override val defaultUser: Username = NewUserManager.defaultUser
  override val defaultPass: Password = NewUserManager.defaultPass

  private val usersTable = quote(querySchema[DataUser]("USERS"))

  def ensureAtLeastOneUserExists(): Future[Unit] = wrapTask("Check users"):
    val isEmpty = run(usersTable.isEmpty)
    if isEmpty then addUserIO(defaultUser, defaultPass)
    else ()

  override def users: Future[Seq[Username]] = performAsync("Get users"):
    run(usersTable.sortBy(_.user).map(_.user))

  override def authenticate(user: Username, pass: Password): Future[Boolean] =
    performAsync("Authenticate"):
      val passHash = hash(user, pass)
      val isDefaultAuth = user == defaultUser && pass == defaultPass
      val userExists = run(
        usersTable
          .filter(u => u.user == lift(user) && u.passHash == lift(passHash))
          .nonEmpty
      )
      val noUsers = run(usersTable.isEmpty)
      userExists || (noUsers && isDefaultAuth)

  override def addUser(user: Username, pass: Password): Future[Option[AlreadyExists]] =
    transactionally(s"Add user $user"):
      addUserIO(user, pass)

  private def addUserIO(user: Username, pass: Password) =
    val exists = run(usersTable.filter(_.user == lift(user)).nonEmpty)
    if exists then Option(AlreadyExists(user))
    else
      run(usersTable.insert(_.user -> lift(user), _.passHash -> lift(hash(user, pass))))
      None

  override def deleteUser(user: Username): Future[Unit] = wrapTask(s"Delete $user"):
    run(tokensTable.filter(_.user == lift(user)).delete)
    run(usersTable.filter(_.user == lift(user)).delete)

  override def updatePassword(user: Username, newPass: Password): Future[Unit] =
    performAsync(s"Update password for $user"):
      run(
        usersTable
          .filter(_.user == lift(user))
          .update(_.passHash -> lift(hash(user, newPass)))
      )

  private def hash(user: Username, pass: Password): String = Auth.hash(user, pass)
