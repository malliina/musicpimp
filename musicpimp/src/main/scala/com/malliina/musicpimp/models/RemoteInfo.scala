package com.malliina.musicpimp.models

import cats.effect.Sync
import com.malliina.http.FullUrl
import com.malliina.musicpimp.http4s.Responses
import com.malliina.musicpimp.json.{MediaRanges, Target}
import com.malliina.play.http.FullUrls2
import com.malliina.values.Username
import org.http4s.{MediaType, Request}

case class RemoteInfo[F[_]](user: Username, apiVersion: MediaType, host: FullUrl, target: Target[F])

object RemoteInfo:
  def forRequest[F[_]: Sync](user: Username, req: Request[?]) =
    RemoteInfo(user, Responses.apiVersion(req), FullUrls2.hostOnly2(req), Target.noop)

  def cloud[F[_]: Sync](user: Username, host: FullUrl) =
    RemoteInfo(user, MediaRanges.JSONv18, host, Target.noop)
