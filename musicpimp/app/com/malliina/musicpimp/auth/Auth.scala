package com.malliina.musicpimp.auth

import com.malliina.musicpimp.db.{DatabaseUserManager, PimpDb}
import com.malliina.values.{Password, Username}
import org.apache.commons.codec.digest.DigestUtils

import scala.concurrent.Future

object Auth {
  def hash(user: Username, password: Password): String =
    hashStrings(user.name, password.pass)

  private def hashStrings(username: String, password: String): String =
    DigestUtils.md5Hex(username + ":" + password)
}

class Auth(db: PimpDb) {
  implicit val ec = db.ec

  /**
    * @return true if credentials were migrated, false otherwise
    */
  def migrateFileCredentialsToDatabaseIfExists(): Future[Boolean] = {
    def bailOut = Future.successful(false)

    val fileUsers = new FileUserManager
    if (fileUsers.hasCredentials()) {
      val dataUsers = new DatabaseUserManager(db)
      fileUsers.savedPassHash.map { fileHash =>
        dataUsers.users.flatMap { users =>
          if (users.isEmpty) {
            val addition = dataUsers addUser DataUser(fileUsers.defaultUser, fileHash)
            addition.map(_ => fileUsers.resetCredentials())
          } else {
            bailOut
          }
        }
      }.getOrElse {
        bailOut
      }
    } else {
      bailOut
    }
  }
}
