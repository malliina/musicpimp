package com.malliina.musicpimp.db

import cats.Functor
import cats.implicits.toFunctorOps
import com.malliina.database.DoobieDatabase
import com.malliina.musicpimp.auth.{Auth, UserManager}
import com.malliina.values.{Password, Username}
import doobie.implicits.toSqlInterpolator
import doobie.free.connection.pure

object DoobieUserManager:
  val defaultUser = Username("admin")
  val defaultPass = Password("test")

  def withUser[F[_]: Functor](db: DoobieDatabase[F]) =
    val users = DoobieUserManager(db)
    users
      .ensureAtLeastOneUserExists()
      .map: _ =>
        users

class DoobieUserManager[F[_]](val db: DoobieDatabase[F])
  extends UserManager[F, Username, Password]
  with DoobieMappings:

  override val defaultUser: Username = DoobieUserManager.defaultUser
  override val defaultPass: Password = DoobieUserManager.defaultPass

  def ensureAtLeastOneUserExists(): F[Unit] = db.run:
    isEmptyQuery.flatMap: isEmpty =>
      if isEmpty then addUserIO(defaultUser, defaultPass).map(_ => ())
      else pure(())

  override def users: F[List[Username]] = db.run:
    sql"""select USER from USERS order by USER""".query[Username].to[List]

  override def authenticate(user: Username, pass: Password): F[Boolean] = db.run:
    val passHash = hash(user, pass)
    val isDefaultAuth = user == defaultUser && pass == defaultPass
    val existsQuery =
      sql"""select exists(select USER from USERS U where U.USER = $user and U.PASS_HASH = $passHash)"""
        .query[Boolean]
        .unique
    for
      userExists <- existsQuery
      noUsers <- isEmptyQuery
    yield userExists || (noUsers && isDefaultAuth)

  override def addUser(user: Username, pass: Password): F[Option[AlreadyExists]] = db.run:
    addUserIO(user, pass)

  override def deleteUser(user: Username): F[Unit] = db.run:
    for
      _ <- sql"""delete from TOKENS where USER = $user""".update.run
      _ <- sql"""delete from USERS where USER = $user""".update.run
    yield ()

  override def updatePassword(user: Username, newPass: Password): F[Unit] = db.run:
    val newPassHash = hash(user, newPass)
    sql"""update USERS set PASS_HASH = $newPassHash where USER = $user""".update.run.map(_ => ())

  private def isEmptyQuery = sql"select not exists(select USER from USERS)".query[Boolean].unique

  private def addUserIO(user: Username, pass: Password) =
    sql"""select exists(select USER from USERS where USER = $user)"""
      .query[Boolean]
      .unique
      .flatMap: exists =>
        if exists then pure(Option(AlreadyExists(user)))
        else
          val passHash = hash(user, pass)
          sql"""insert into USERS(USER, PASS_HASH) values($user, $passHash)""".update.run.map: _ =>
            None

  private def hash(user: Username, pass: Password): String = Auth.hash(user, pass)
