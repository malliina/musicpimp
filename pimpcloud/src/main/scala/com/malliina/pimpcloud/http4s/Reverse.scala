package com.malliina.pimpcloud.http4s

import com.malliina.musicpimp.models.{FolderID, TrackID}
import org.http4s.Uri
import org.http4s.implicits.uri

class Segment(val base: Uri)

object Reverse extends Reverse

case class GoogleUris(returnUri: Uri, oauth: Uri, callback: Uri)

object GoogleUris:
  def apply(reverse: Reverse): GoogleUris =
    GoogleUris(reverse.admin.base, reverse.oauth, reverse.oauthcb)

trait Reverse:
  val root = uri"/"
  val authenticate = uri"/authenticate"
  val login = uri"/login"
  object admin extends Segment(uri"/admin"):
    val logs = base / "logs"
    val logout = base / "logout"
    val eject = base / "eject"
  object folders extends Segment(uri"/folders"):
    def folder(id: FolderID) = base / id.id
  object downloads extends Segment(uri"/downloads"):
    def download(id: TrackID) = base / id.id
  val search = uri"/search"
  val oauth = uri"/oauth"
  val oauthcb = uri"/oauthcb"
