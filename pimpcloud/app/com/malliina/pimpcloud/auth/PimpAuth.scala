package com.malliina.pimpcloud.auth

import com.malliina.musicpimp.models.CloudID
import com.malliina.play.auth.Auth
import com.malliina.values.{Password, Username}
import play.api.mvc.RequestHeader

object PimpAuth {
  def cloudCredentials(request: RequestHeader): Option[CloudCredentials] = {
    Auth.authHeaderParser(request) { decoded =>
      decoded.split(":", 3) match {
        case Array(cloudID, user, pass) =>
          Some(CloudCredentials(CloudID(cloudID), Username(user), Password(pass), request))
        case _ =>
          None
      }
    }
  }
}

case class CloudCredentials(cloudID: CloudID,
                            username: Username,
                            password: Password,
                            rh: RequestHeader)
