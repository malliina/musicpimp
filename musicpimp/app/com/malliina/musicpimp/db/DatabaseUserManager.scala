package com.malliina.musicpimp.db

import java.sql.SQLException

import com.malliina.musicpimp.auth.{Auth, DataUser, UserManager}
import com.malliina.values.{Password, Username}

import scala.concurrent.Future

object DatabaseUserManager {
  val DefaultUser = Username("admin")
  val DefaultPass = Password("test")
}

class DatabaseUserManager(val db: PimpDb) extends UserManager[Username, Password] {
  implicit val ec = db.ec

  import db.api._
  import db.api.username
  import db.schema.{tokens, usersTable}

  def ensureAtLeastOneUserExists(): Future[Unit] = {
    users flatMap { us =>
      if (us.isEmpty) addUser(defaultUser, defaultPass).map(_ => ())
      else Future.successful(())
    }
  }

  override def defaultUser: Username = DatabaseUserManager.DefaultUser

  override def defaultPass: Password = DatabaseUserManager.DefaultPass

  protected def usersQuery = usersTable.map(_.user)

  override def authenticate(user: Username, pass: Password): Future[Boolean] = {
    val passHash = hash(user, pass)
    val userExists = usersTable.filter(u => u.user === user && u.passHash === passHash).exists
    for {
      exists <- db.database.run(userExists.result)
      usersExist <- db.database.run(usersQuery.exists.result)
    } yield exists || (user == defaultUser && pass == defaultPass && !usersExist)
  }

  /**
    * @return all users
    */
  override def users: Future[Seq[Username]] = db.runQuery(usersQuery)

  override def addUser(user: Username, pass: Password): Future[Option[AlreadyExists]] =
    addUser(DataUser(user, hash(user, pass)))

  def addUser(user: DataUser): Future[Option[AlreadyExists]] = {
    db.run(usersTable += user).map(_ => None) recover {
      case sqle: SQLException if sqle.getMessage contains "primary key violation" =>
        Some(AlreadyExists(user.username))
    }
  }

  override def deleteUser(user: Username): Future[Unit] =
    db.run {
      DBIO.seq(
        tokens.filter(_.user === user).delete,
        usersTable.filter(_.user === user).delete
      )
    }

  override def updatePassword(user: Username, newPass: Password): Future[Unit] =
    db.run(usersTable
      .filter(u => u.user === user)
      .map(_.passHash)
      .update(hash(user, newPass))).map(_ => ())

  private def hash(user: Username, pass: Password) = Auth.hash(user, pass)
}
