package com.mle.musicpimp.auth

import java.io.FileNotFoundException
import java.nio.file.Files

import com.mle.file.FileUtilities
import com.mle.musicpimp.util.FileUtil
import com.mle.util.Utils

import scala.io.BufferedSource


class FileUserManager extends UserManager {
  private val passFile = FileUtilities pathTo "credentials.txt"
  val defaultUser = "admin"
  val defaultPass = "test"

  def savedPassHash: Option[User] =
    Utils.opt[BufferedSource, FileNotFoundException](scala.io.Source.fromFile(passFile.toFile))
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
  override def authenticate(user: User, pass: Password): Boolean = {
    savedPassHash.fold(ifEmpty = user == defaultUser && pass == defaultPass)(_ == Auth.hash(user, pass))
  }

  override def deleteUser(user: User): Unit = ()

  override def users: Seq[User] = Seq(defaultUser)

  override def updatePassword(user: User, newPass: Password): Unit = {
    FileUtilities.writerTo(passFile)(passWriter => {
      passWriter write Auth.hash(user, newPass)
    })
    FileUtil trySetOwnerOnlyPermissions passFile
  }

  override def addUser(user: User, pass: Password): Option[AlreadyExists] = None

  def hasCredentials(): Boolean = Files.exists(passFile)

  def resetCredentials() = Files.deleteIfExists(passFile)
}