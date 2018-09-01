package com.malliina.musicpimp.models

import com.malliina.http.FullUrl
import com.malliina.musicpimp.http.PimpRequest
import com.malliina.musicpimp.json.{JsonFormatVersions, Target}
import com.malliina.play.http.FullUrls
import com.malliina.values.Username
import play.api.mvc.RequestHeader

case class RemoteInfo(user: Username, apiVersion: String, host: FullUrl, target: Target)

object RemoteInfo {
  def forRequest(user: Username, rh: RequestHeader) =
    RemoteInfo(user, PimpRequest.apiVersion(rh), FullUrls.hostOnly(rh), Target.noop)

  def cloud(user: Username, host: FullUrl) =
    RemoteInfo(user, JsonFormatVersions.JSONv18, host, Target.noop)
}
