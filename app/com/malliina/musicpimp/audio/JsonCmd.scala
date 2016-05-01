package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.JsonStrings._
import play.api.libs.json.{JsValue, Reads}

class JsonCmd(json: JsValue) {
  val command = (json \ CMD).validate[String]

  def intValue = asValue[Int]

  def doubleValue = asValue[Double]

  def boolValue = asValue[Boolean]

  def stringValue = asValue[String]

  def asValue[T : Reads] = (json \ VALUE).validate[T]

  def track = (json \ TRACK).validate[String]

  def tracks = (json \ TRACKS).validate[Seq[String]]

  def tracksOrNil = tracks getOrElse Nil

  def folders = (json \ FOLDERS).validate[Seq[String]]

  def foldersOrNil = folders getOrElse Nil

  def id = (json \ ID).validate[String]

  def index = (json \ PLAYLIST_INDEX).validate[Int]

  def indexOrValue = index orElse intValue
}
