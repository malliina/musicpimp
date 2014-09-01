package com.mle.musicpimp.db

import com.mle.storage.StorageLong

import scala.concurrent.duration.DurationInt
import scala.slick.driver.H2Driver.simple._

/**
 * @author Michael
 */
class Tracks(tag: Tag) extends Table[DataTrack](tag, "TRACKS") {
  val toTrack: (String, String, String, String, Int, Long) => DataTrack = (i, ar, al, tr, du, si) => DataTrack(i, ar, al, tr, du.seconds, si.bytes)

  def id = column[String]("ID", O.PrimaryKey)

  def title = column[String]("TITLE")

  def artist = column[String]("ARTIST")

  def album = column[String]("ALBUM")

  def duration = column[Int]("DURATION")

  def size = column[Long]("SIZE")

  def * = (id, title, artist, album, duration, size) <>((DataTrack.fromValues _).tupled, (t: DataTrack) => t.toValues)

  val tmp = DataTrack.apply _
  //  val tmp2:(String, String, String, String, Duration, StorageSize) => DataTrack =
}