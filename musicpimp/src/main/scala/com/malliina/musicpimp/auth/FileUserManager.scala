package com.malliina.musicpimp.auth

import cats.effect.Sync

import java.io.FileNotFoundException
import java.nio.file.Files
import com.malliina.file.FileUtilities
import com.malliina.musicpimp.util.FileUtil
import com.malliina.values.Literals.{pass, user}
import com.malliina.values.{Password, Username}

class FileUserManager[F[_]: Sync] extends UserManager[F, Username, Password]:
  val F = Sync[F]
  private val passFile = FileUtilities.pathTo("credentials.txt")
  val defaultUser: Username = user"admin"
  val defaultPass: Password = pass"test"

  def savedPassHash: Option[String] =
    try
      val src = scala.io.Source.fromFile(passFile.toFile)
      src.getLines().toList.headOption
    catch case e: FileNotFoundException => None

  /** Validates the supplied credentials.
    *
    * Hashes the supplied credentials and compares the hash with the first line of the password
    * file. If they equal, validation succeeds, otherwise it fails.
    *
    * If the password file doesn't exist, it means no password has ever been set thus the
    * credentials are compared against the default credentials.
    *
    * @param user
    *   the supplied username
    * @param pass
    *   the supplied password
    * @return
    *   true if the credentials are valid, false otherwise
    */
  override def authenticate(user: Username, pass: Password): F[Boolean] = pure:
    savedPassHash.fold(ifEmpty = user == defaultUser && pass == defaultPass)(
      _ == Auth.hash(user, pass)
    )

  override def deleteUser(user: Username): F[Unit] = pure(())

  override def users: F[List[Username]] = F.pure(List(defaultUser))

  override def updatePassword(user: Username, newPass: Password): F[Unit] = pure:
    FileUtilities.writerTo(passFile): passWriter =>
      passWriter.write(Auth.hash(user, newPass))
    FileUtil.trySetOwnerOnlyPermissions(passFile)

  override def addUser(user: Username, pass: Password): F[Option[AlreadyExists]] = pure(None)

  def hasCredentials(): Boolean = Files.exists(passFile)

  def resetCredentials() = Files.deleteIfExists(passFile)

  def pure[T](t: T) = F.pure(t)
