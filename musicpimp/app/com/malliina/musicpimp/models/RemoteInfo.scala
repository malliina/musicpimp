package com.malliina.musicpimp.models

import com.malliina.play.http.{FullUrl, RequestInfo}
import com.malliina.play.models.Username

case class RemoteInfo(user: Username, host: FullUrl)

object RemoteInfo {
  def apply(req: RequestInfo[Username]): RemoteInfo =
    RemoteInfo(req.user, FullUrl.hostOnly(req.request))
}
