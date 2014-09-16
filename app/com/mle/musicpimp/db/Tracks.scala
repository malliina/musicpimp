package com.mle.musicpimp.db

import scala.slick.driver.H2Driver.simple._

/**
 * @author Michael
 */
class Tracks(tag: Tag) extends Table[DataTrack](tag, "TRACKS") {
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

  // foreign key to itself; the root folder is its own parent
  def parentFolder = foreignKey("PARENT_FK", parent, PimpDb.folders)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

  def * = (id, title, path, parent) <>((DataFolder.apply _).tupled, DataFolder.unapply)
}
