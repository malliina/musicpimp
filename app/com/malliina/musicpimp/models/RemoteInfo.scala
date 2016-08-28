package com.malliina.musicpimp.models

import com.malliina.play.http.RequestInfo

case class RemoteInfo(user: User, host: PimpUrl)

object RemoteInfo {
  def apply(req: RequestInfo[User]): RemoteInfo =
    RemoteInfo(req.user, PimpUrl.hostOnly(req.request))
}
