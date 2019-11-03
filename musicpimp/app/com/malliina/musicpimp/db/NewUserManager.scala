package com.malliina.musicpimp.db

import com.malliina.musicpimp.auth.{Auth, DataUser, UserManager}
import com.malliina.values.{Password, Username}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object NewUserManager {
  val defaultUser = Username("admin")
  val defaultPass = Password("test")

  def withUser(db: PimpMySQL): NewUserManager = {
    val users = apply(db)
    Await.result(users.ensureAtLeastOneUserExists(), 10.seconds)
    users
  }

  def apply(db: PimpMySQL): NewUserManager = new NewUserManager(db)
}

class NewUserManager(val db: PimpMySQL) extends UserManager[Username, Password] {
  import db._

  override val defaultUser: Username = NewUserManager.defaultUser
  override val defaultPass: Password = NewUserManager.defaultPass

  val usersTable = quote(querySchema[DataUser]("USERS"))

  def ensureAtLeastOneUserExists(): Future[Unit] = performAsync("Check users") {
    for {
      isEmpty <- runIO(usersTable.isEmpty)
      _ <- if (isEmpty) addUserIO(defaultUser, defaultPass).map(_ => ())
      else IO.successful[Unit](())
    } yield ()
  }

  override def users: Future[Seq[Username]] = performAsync("Get users") {
    runIO(usersTable.sortBy(_.user).map(_.user))
  }

  override def authenticate(user: Username, pass: Password): Future[Boolean] =
    performAsync("Authenticate") {
      val passHash = hash(user, pass)
      val isDefaultAuth = user == defaultUser && pass == defaultPass
      for {
        userExists <- runIO(
          usersTable
            .filter(u => u.user == lift(user) && u.passHash == lift(passHash))
            .nonEmpty
        )
        noUsers <- runIO(usersTable.isEmpty)
      } yield userExists || (noUsers && isDefaultAuth)
    }

  override def addUser(user: Username, pass: Password): Future[Option[AlreadyExists]] =
    transactionally(s"Add user $user") {
      addUserIO(user, pass)
    }

  private def addUserIO(user: Username, pass: Password) =
    runIO(usersTable.filter(_.user == lift(user)).nonEmpty).flatMap { exists =>
      if (exists) {
        IO.successful(Option(AlreadyExists(user)))
      } else {
        runIO(
          usersTable.insert(_.user -> lift(user), _.passHash -> lift(hash(user, pass)))
        ).map(_ => None)
      }
    }

  override def deleteUser(user: Username): Future[Unit] = transactionally(s"Delete $user") {
    IO.sequence(
        List(
          runIO(tokensTable.filter(_.user == lift(user)).delete),
          runIO(usersTable.filter(_.user == lift(user)).delete)
        )
      )
      .map(_ => ())
  }

  override def updatePassword(user: Username, newPass: Password): Future[Unit] =
    performAsync(s"Update password for $user") {
      runIO(
        usersTable
          .filter(_.user == lift(user))
          .update(_.passHash -> lift(hash(user, newPass)))
      ).map(_ => ())
    }

  private def hash(user: Username, pass: Password): String = Auth.hash(user, pass)
}
