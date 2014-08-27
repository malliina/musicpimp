package com.mle.musicpimp.db

import scala.slick.driver.H2Driver.simple._

/**
 * @author Michael
 */
class Tracks(tag: Tag) extends Table[DataTrack](tag, "TRACKS") {
  def id = column[String]("ID", O.PrimaryKey)

  def artist = column[String]("ARTIST")

  def album = column[String]("ALBUM")

  def track = column[String]("TRACK")

  def * = (id, artist, album, track) <>(DataTrack.tupled, DataTrack.unapply)
}