package com.mle.musicpimp.auth

import com.mle.musicpimp.db.DatabaseUserManager
import org.apache.commons.codec.digest.DigestUtils

/**
 * @author Michael
 */
object Auth {
  def hash(username: String, password: String) = DigestUtils.md5Hex(username + ":" + password)

  def migrateFileCredentialsToDatabaseIfExists(): Boolean = {
    val fileUsers = new FileUserManager
    if (fileUsers.hasCredentials()) {
      val dataUsers = new DatabaseUserManager
      fileUsers.savedPassHash.filter(_ => dataUsers.users.isEmpty).map(fileHash => {
        dataUsers addUser DataUser(fileUsers.defaultUser, fileHash)
        fileUsers.resetCredentials()
      }).contains(true)
    } else {
      false
    }
  }
}
