package com.malliina.musicpimp.db

import com.malliina.musicpimp.db.Mappings.jodaDate
import com.malliina.musicpimp.models.User
import org.joda.time.DateTime

import slick.driver.H2Driver.api._
import slick.lifted.ProvenShape

case class PlaybackRecord(track: String, when: DateTime, user: User)

class Plays(tag: Tag) extends Table[PlaybackRecord](tag, "PLAYS") {
  def track = column[String]("TRACK")

  def when = column[DateTime]("WHEN")

  def who = column[User]("WHO")

  def trackConstraint = foreignKey("TRACK_FK", track, PimpSchema.tracks)(
    _.id,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.NoAction
  )

  def whoConstraint = foreignKey("WHO_FK", who, PimpSchema.usersTable)(
    _.user,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade
  )

  def * : ProvenShape[PlaybackRecord] =
    (track, when, who) <>((PlaybackRecord.apply _).tupled, PlaybackRecord.unapply)
}

class Tracks(tag: Tag) extends Table[DataTrack](tag, "TRACKS") {
  def id = column[String]("ID", O.PrimaryKey)

  def title = column[String]("TITLE")

  def artist = column[String]("ARTIST")

  def album = column[String]("ALBUM")

  def duration = column[Int]("DURATION")

  def size = column[Long]("SIZE")

  def folder = column[String]("FOLDER")

  def folderConstraint = foreignKey("FOLDER_FK", folder, PimpSchema.folders)(
    _.id,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade)

  def * = (id, title, artist, album, duration, size, folder) <>((DataTrack.fromValues _).tupled, (t: DataTrack) => t.toValues)
}

class Folders(tag: Tag) extends Table[DataFolder](tag, "FOLDERS") {
  def id = column[String]("ID", O.PrimaryKey)

  def title = column[String]("TITLE")

  def path = column[String]("PATH")

  def parent = column[String]("PARENT")

  // foreign key to itself; the root folder is its own parent
  def parentFolder = foreignKey("PARENT_FK", parent, PimpSchema.folders)(
    _.id,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade)

  def * = (id, title, path, parent) <>((DataFolder.apply _).tupled, DataFolder.unapply)
}

/**
  * Temp table.
  */
class Ids(tag: Tag) extends Table[Id](tag, "IDS") {
  def id = column[String]("ID", O.PrimaryKey)

  def * = id <>(Id.apply, Id.unapply)
}

case class Id(id: String)
