package com.malliina.musicpimp.models

import com.malliina.play.http.RequestInfo

case class RemoteInfo(user: String, host: PimpUrl)

object RemoteInfo {
  def apply(req: RequestInfo): RemoteInfo =
    RemoteInfo(req.user, PimpUrl.hostOnly(req.request))
}
