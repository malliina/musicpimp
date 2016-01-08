package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.JsonStrings._
import play.api.libs.json.JsValue

/**
  *
  * @author mle
  */
class JsonCmd(json: JsValue) {
  val command = (json \ CMD).as[String]

  def value = (json \ VALUE).as[Int]

  def boolValue = (json \ VALUE).as[Boolean]

  def stringValue = (json \ VALUE).as[String]

  def trackOpt = (json \ TRACK).asOpt[String]

  def track = trackOpt.get

  def tracksOpt = (json \ TRACKS).asOpt[Seq[String]]

  def tracksOrNil = tracksOpt getOrElse Nil

  def foldersOpt = (json \ FOLDERS).asOpt[Seq[String]]

  def foldersOrNil = foldersOpt getOrElse Nil

  def idOpt = (json \ ID).asOpt[String]

  def id = idOpt.get

  def indexOpt = (json \ PLAYLIST_INDEX).asOpt[Int]

  def indexOrValue = indexOpt getOrElse value
}