package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.models.{FolderID, TrackID}
import play.api.libs.json.{JsValue, Reads}

class JsonCmd(json: JsValue) {
  val command = key[String](Cmd)

  def intValue = asValue[Int]

  def doubleValue = asValue[Double]

  def boolValue = asValue[Boolean]

  def stringValue = asValue[String]

  def asValue[T: Reads] = key[T](Value)

  def track = key[TrackID](TrackKey)

  def tracks = key[Seq[TrackID]](Tracks)

  def tracksOrNil = tracks getOrElse Nil

  def folders = key[Seq[FolderID]](Folders)

  def foldersOrNil = folders getOrElse Nil

  def id = key[String](Id)

  def index = key[Int](PlaylistIndex)

  def indexOrValue = index orElse intValue

  def key[T: Reads](key: String) = (json \ key).validate[T]
}
