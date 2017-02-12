package com.malliina.musicpimp.models

import com.malliina.play.http.RequestInfo
import com.malliina.play.models.Username

case class RemoteInfo(user: Username, host: PimpUrl)

object RemoteInfo {
  def apply(req: RequestInfo[Username]): RemoteInfo =
    RemoteInfo(req.user, PimpUrl.hostOnly(req.request))
}
