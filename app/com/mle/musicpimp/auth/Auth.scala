package com.mle.musicpimp.auth

import com.mle.musicpimp.db.{DatabaseUserManager, PimpDb}
import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
  * @author Michael
  */
object Auth {
  def hash(username: String, password: String) = DigestUtils.md5Hex(username + ":" + password)
}

class Auth(db: PimpDb) {
  /**
    * @return true if credentials were migrated, false otherwise
    */
  def migrateFileCredentialsToDatabaseIfExists(): Future[Boolean] = {
    def bailOut = Future.successful(false)
    val fileUsers = new FileUserManager
    if (fileUsers.hasCredentials()) {
      val dataUsers = new DatabaseUserManager(db)
      fileUsers.savedPassHash.map(fileHash => {
        dataUsers.users.flatMap(users => {
          if (users.isEmpty) {
            val addition = dataUsers addUser DataUser(fileUsers.defaultUser, fileHash)
            addition.map(_ => fileUsers.resetCredentials())
          } else {
            bailOut
          }
        })
      }).getOrElse {
        bailOut
      }
    } else {
      bailOut
    }
  }
}
