package com.malliina.musicpimp.db

import java.sql.SQLException

import com.malliina.musicpimp.auth.{Auth, DataUser, UserManager}
import com.malliina.musicpimp.models.User
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.H2Driver.api._

import scala.concurrent.Future

object DatabaseUserManager {
  val DefaultUser = User("admin")
}

class DatabaseUserManager(db: PimpDb) extends UserManager[User, String] {

  import PimpSchema.{tokens, usersTable}

  def ensureAtLeastOneUserExists(): Future[Unit] = {
    users.flatMap(us => {
      if (us.isEmpty) addUser(defaultUser, defaultPass).map(_ => ())
      else Future.successful(())
    })
  }

  override def defaultUser: User = DatabaseUserManager.DefaultUser

  override def defaultPass: String = "test"

  protected def usersQuery = usersTable.map(_.user)

  override def authenticate(user: User, pass: String): Future[Boolean] = {
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
  override def users: Future[Seq[User]] = db.runQuery(usersQuery)

  override def addUser(user: User, pass: String): Future[Option[AlreadyExists]] =
    addUser(DataUser(user, hash(user, pass)))

  def addUser(user: DataUser): Future[Option[AlreadyExists]] = {
    db.run(usersTable += user).map(_ => None).recover {
      case sqle: SQLException if sqle.getMessage contains "primary key violation" =>
        Some(AlreadyExists(user.username))
    }
  }

  override def deleteUser(user: User): Future[Unit] = {
    db.run {
      DBIO.seq(
        tokens.filter(_.user === user.name).delete,
        usersTable.filter(_.user === user).delete
      )
    }
  }

  override def updatePassword(user: User, newPass: String): Future[Unit] =
    db.run(usersTable.filter(u => u.user === user).map(_.passHash).update(hash(user, newPass))).map(_ => ())

  private def hash(user: User, pass: String) = Auth.hash(user, pass)
}
