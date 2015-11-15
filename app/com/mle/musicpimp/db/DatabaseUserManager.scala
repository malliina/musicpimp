package com.mle.musicpimp.db

import java.sql.SQLException

import com.mle.musicpimp.auth.{Auth, DataUser, UserManager}

import scala.concurrent.Future
import scala.slick.driver.H2Driver.simple._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * @author Michael
 */
class DatabaseUserManager(db: PimpDb) extends UserManager {

  import PimpSchema.{tokens, usersTable}

  def withSession[T](f: Session => T): Future[T] = db.withSession(f)

  def ensureAtLeastOneUserExists(): Future[Unit] = {
    users.flatMap(us => {
      if(us.isEmpty) addUser(defaultUser, defaultPass).map(_ => ())
      else Future.successful(())
    })
  }

  override def defaultUser: User = "admin"

  override def defaultPass: Password = "test"

  protected def usersQuery = usersTable.map(_.user)

  override def authenticate(user: User, pass: Password): Future[Boolean] = {
    withSession(s => {
      val passHash = hash(user, pass)
      val userExists = usersTable.filter(u => u.user === user && u.passHash === passHash).exists.run(s)
      userExists || (user == defaultUser && pass == defaultPass && !usersQuery.exists.run(s))
    })
  }

  /**
   * @return all users
   */
  override def users: Future[Seq[User]] = withSession(s => usersQuery.list(s))

  override def addUser(user: User, pass: Password): Future[Option[AlreadyExists]] = addUser(DataUser(user, hash(user, pass)))

  def addUser(user: DataUser): Future[Option[AlreadyExists]] = {
    withSession(implicit s => usersTable += user).map(_ => None).recover {
      case sqle: SQLException if sqle.getMessage contains "primary key violation" => Some(AlreadyExists(user.username))
    }
  }

  override def deleteUser(user: User): Future[Unit] = {
    withSession(implicit s => {
      tokens.filter(_.user === user).delete
      usersTable.filter(_.user === user).delete
    })
  }

  override def updatePassword(user: User, newPass: Password): Future[Unit] = {
    withSession(s => {
      usersTable.filter(u => u.user === user).map(_.passHash).update(hash(user, newPass))(s)
    })
  }

  private def hash(user: User, pass: Password) = Auth.hash(user, pass)
}
