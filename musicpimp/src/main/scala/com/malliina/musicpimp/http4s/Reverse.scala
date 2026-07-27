package com.malliina.musicpimp.http4s

import com.malliina.musicpimp.models.{FolderID, PlaylistID, TrackID}
import com.malliina.values.Username
import org.http4s.Uri
import org.http4s.implicits.uri

class Segment(val base: Uri)

object Reverse extends Reverse

trait Reverse:
  object website extends Segment(uri"/player"):
    val player = base
    val popular = base / "popular"
    val recent = base / "recent"

  object folders extends Segment(uri"/folders"):
    def folder(id: FolderID) = base / id.id
  object tracks extends Segment(uri"/tracks"):
    val folders = base / "folders"
    def folder(id: FolderID) = folders / id.id
    def meta(id: TrackID) = folders / "meta" / id.id
    def track(id: TrackID) = base / id.id

  def downloads(id: TrackID) = uri"/downloads" / id.id

  val settings = uri"/settings"
  val connect = uri"/connect"
  val image = uri"/image"
  val cloud = uri"/cloud"
  val about = uri"/about"
  object manage extends Segment(uri"/manage"):
    object push extends Segment(base / "push"):
      val tokens = base / "tokens"
      val remove = tokens / "deletes"
  val account = uri"/account"
  object users extends Segment(uri"/users"):
    def delete(username: Username) = base / "delete" / username.name
  val login = uri"/login"
  val logout = uri"/logout"
  val authenticate = uri"/authenticate"
  val changePassword = uri"/changePassword"
  val addUser = uri"/addUser"
  object playback extends Segment(uri"/playback"):
    val uploads = base / "uploads"
    val stream = base / "stream"
    val server = base / "server"
  object alarms extends Segment(uri"/alarms"):
    val editor = base / "editor"
    def edit(id: String) = editor / id
    def add = editor / "add"
  object logs extends Segment(uri"/logs"):
    val levels = base / "levels"
  object playlists extends Segment(uri"/playlists"):
    def playlist(id: PlaylistID) = base / id.id
    def delete(id: PlaylistID) = base / "delete" / id.id
    val edit = base / "edit"
    val handle = base / "handle"
  val search = uri"/search"
  object rootfolders extends Segment(uri"/rootfolders"):
    def delete(id: String) = base / "delete" / id
