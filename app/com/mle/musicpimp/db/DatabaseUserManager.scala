package com.mle.musicpimp.db

import java.sql.SQLException

import com.mle.musicpimp.auth.{Auth, DataUser, UserManager}

import scala.slick.driver.H2Driver.simple._

/**
 * @author Michael
 */
class DatabaseUserManager extends UserManager {

  import com.mle.musicpimp.db.PimpDb.{tokens, usersTable, withSession}

  def ensureAtLeastOneUserExists(): Unit = {
    if (users.isEmpty) {
      addUser(defaultUser, defaultPass)
    }
  }

  override def defaultUser: User = "admin"

  override def defaultPass: Password = "test"

  protected def usersQuery = usersTable.map(_.user)

  override def authenticate(user: User, pass: Password): Boolean = withSession(s => {
    val passHash = hash(user, pass)
    val userExists = usersTable.filter(u => u.user === user && u.passHash === passHash).exists.run(s)
    userExists || (user == defaultUser && pass == defaultPass && !usersQuery.exists.run(s))
  })

  /**
   * @return all users
   */
  override def users: Seq[User] = withSession(s => usersQuery.list(s))

  override def addUser(user: User, pass: Password): Option[AlreadyExists] = addUser(DataUser(user, hash(user, pass)))

  def addUser(user: DataUser) =
    try {
      withSession(implicit s => usersTable += user)
      None
    } catch {
      case sqle: SQLException if sqle.getMessage contains "primary key violation" => Some(AlreadyExists(user.username))
    }

  override def deleteUser(user: User): Unit =
    withSession(implicit s => {
      tokens.filter(_.user === user).delete
      usersTable.filter(_.user === user).delete
    })

  override def updatePassword(user: User, newPass: Password): Unit = withSession(s => {
    usersTable.filter(u => u.user === user).map(_.passHash).update(hash(user, newPass))(s)
  })

  private def hash(user: User, pass: Password) = Auth.hash(user, pass)
}
