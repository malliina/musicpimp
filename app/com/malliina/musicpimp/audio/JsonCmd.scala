package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.models.{FolderID, TrackID}
import play.api.libs.json.{JsValue, Reads}

class JsonCmd(json: JsValue) {
  val command = (json \ Cmd).validate[String]

  def intValue = asValue[Int]

  def doubleValue = asValue[Double]

  def boolValue = asValue[Boolean]

  def stringValue = asValue[String]

  def asValue[T : Reads] = (json \ Value).validate[T]

  def track = (json \ TrackKey).validate[TrackID]

  def tracks = (json \ Tracks).validate[Seq[TrackID]]

  def tracksOrNil = tracks getOrElse Nil

  def folders = (json \ Folders).validate[Seq[FolderID]]

  def foldersOrNil = folders getOrElse Nil

  def id = (json \ Id).validate[String]

  def index = (json \ PlaylistIndex).validate[Int]

  def indexOrValue = index orElse intValue
}
