package com.mle.musicpimp.db

import com.mle.storage.StorageLong

import scala.concurrent.duration.DurationInt
import scala.slick.driver.H2Driver.simple._

/**
 * @author Michael
 */
class Tracks(tag: Tag) extends Table[DataTrack](tag, "TRACKS") {
  val toTrack: (String, String, String, String, Int, Long, String) => DataTrack =
    (i, ti, ar, al, du, si, fo) => DataTrack(i, ti, ar, al, du.seconds, si.bytes, fo)

  def id = column[String]("ID", O.PrimaryKey)

  def title = column[String]("TITLE")

  def artist = column[String]("ARTIST")

  def album = column[String]("ALBUM")

  def duration = column[Int]("DURATION")

  def size = column[Long]("SIZE")

  def folder = column[String]("FOLDER")

  def folderConstraint = foreignKey("FOLDER_FK", folder, PimpDb.folders)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

  def * = (id, title, artist, album, duration, size, folder) <>((DataTrack.fromValues _).tupled, (t: DataTrack) => t.toValues)
}

class Folders(tag: Tag) extends Table[DataFolder](tag, "FOLDERS") {
  def id = column[String]("ID", O.PrimaryKey)

  def title = column[String]("TITLE")

  def path = column[String]("PATH")

  def parent = column[String]("PARENT")

  // foreign key to itself
  def parentFolder = foreignKey("PARENT_FK", parent, PimpDb.folders)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

  def * = (id, title, path, parent) <>((DataFolder.apply _).tupled, DataFolder.unapply)
}
