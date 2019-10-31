package com.malliina.musicpimp.auth

import java.io.FileNotFoundException
import java.nio.file.Files

import com.malliina.file.FileUtilities
import com.malliina.musicpimp.util.FileUtil
import com.malliina.util.Utils
import com.malliina.values.{Password, Username}

import scala.concurrent.Future
import scala.io.BufferedSource

class FileUserManager extends UserManager[Username, Password] {
  private val passFile = FileUtilities pathTo "credentials.txt"
  val defaultUser = Username("admin")
  val defaultPass = Password("test")

  def savedPassHash: Option[String] =
    Utils
      .opt[BufferedSource, FileNotFoundException](scala.io.Source.fromFile(passFile.toFile))
      .flatMap(_.getLines().toList.headOption)

  /**
    * Validates the supplied credentials.
    *
    * Hashes the supplied credentials and compares the hash with the first line of
    * the password file. If they equal, validation succeeds, otherwise it fails.
    *
    * If the password file doesn't exist, it means no password has ever been set thus
    * the credentials are compared against the default credentials.
    *
    * @param user the supplied username
    * @param pass the supplied password
    * @return true if the credentials are valid, false otherwise
    */
  override def authenticate(user: Username, pass: Password): Future[Boolean] = fut {
    savedPassHash.fold(ifEmpty = user == defaultUser && pass == defaultPass)(
      _ == Auth.hash(user, pass)
    )
  }

  override def deleteUser(user: Username): Future[Unit] = fut(())

  override def users: Future[Seq[Username]] = fut(Seq(defaultUser))

  override def updatePassword(user: Username, newPass: Password): Future[Unit] = fut {
    FileUtilities.writerTo(passFile)(passWriter => {
      passWriter write Auth.hash(user, newPass)
    })
    FileUtil trySetOwnerOnlyPermissions passFile
  }

  override def addUser(user: Username, pass: Password): Future[Option[AlreadyExists]] = fut(None)

  def hasCredentials(): Boolean = Files.exists(passFile)

  def resetCredentials() = Files.deleteIfExists(passFile)

  def fut[T](t: T) = Future.successful(t)
}
