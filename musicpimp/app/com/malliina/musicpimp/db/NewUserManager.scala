package com.malliina.musicpimp.db

import com.malliina.musicpimp.auth.{Auth, DataUser, UserManager}
import com.malliina.values.{Password, Username}

import scala.concurrent.Future

class NewUserManager(val db: PimpMySQL) extends UserManager[Username, Password] {
  import db._
  val defaultUser = Username("admin")
  val defaultPass = Password("test")

  val usersTable = quote(querySchema[DataUser]("USERS"))

  def ensureAtLeastOneUserExists(): Future[Unit] = performAsync("Check users") {
    for {
      isEmpty <- runIO(usersTable.isEmpty)
      _ <- if (isEmpty) addUserIO(defaultUser, defaultPass).map(_ => ())
      else IO.successful[Unit](())
    } yield ()
  }

  override def users: Future[Seq[Username]] = performAsync("Get users") {
    runIO(usersTable.sortBy(_.username).map(_.username))
  }

  override def authenticate(user: Username, pass: Password): Future[Boolean] =
    performAsync("Authenticate") {
      val passHash = hash(user, pass)
      val isDefaultAuth = user == defaultUser && pass == defaultPass
      for {
        userExists <- runIO(
          usersTable
            .filter(u => u.username == lift(user) && u.passwordHash == lift(passHash))
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
    runIO(usersTable.filter(_.username == lift(user)).nonEmpty).flatMap { exists =>
      if (exists) {
        IO.successful(Option(AlreadyExists(user)))
      } else {
        runIO(
          usersTable.insert(_.username -> lift(user), _.passwordHash -> lift(hash(user, pass)))
        ).map(_ => None)
      }
    }

  override def deleteUser(user: Username): Future[Unit] = transactionally(s"Delete $user") {
    IO.sequence(
        List(
          runIO(tokensTable.filter(_.user == lift(user)).delete),
          runIO(usersTable.filter(_.username == lift(user)).delete)
        )
      )
      .map(_ => ())
  }

  override def updatePassword(user: Username, newPass: Password): Future[Unit] =
    performAsync(s"Update password for $user") {
      runIO(
        usersTable
          .filter(_.username == lift(user))
          .update(_.passwordHash -> lift(hash(user, newPass)))
      ).map(_ => ())
    }

  private def hash(user: Username, pass: Password): String = Auth.hash(user, pass)
}
