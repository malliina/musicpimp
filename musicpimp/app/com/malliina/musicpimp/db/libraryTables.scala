package com.malliina.musicpimp.db

import java.time.Instant

import com.malliina.musicpimp.db.Mappings.{instant, username}
import com.malliina.musicpimp.models.{FolderID, PimpPath, TrackID}
import com.malliina.play.models.Username
import slick.jdbc.H2Profile.api._
import slick.lifted.ProvenShape

case class PlaybackRecord(track: TrackID, when: Instant, user: Username)

class Plays(tag: Tag) extends Table[PlaybackRecord](tag, "PLAYS") {
  def track = column[TrackID]("TRACK")

  def when = column[Instant]("WHEN")

  def who = column[Username]("WHO")

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
  def id = column[TrackID]("ID", O.PrimaryKey)

  def title = column[String]("TITLE")

  def artist = column[String]("ARTIST")

  def album = column[String]("ALBUM")

  def duration = column[Int]("DURATION")

  def size = column[Long]("SIZE")

  def folder = column[FolderID]("FOLDER")

  def folderConstraint = foreignKey("FOLDER_FK", folder, PimpSchema.folders)(
    _.id,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade)

  def * = (id, title, artist, album, duration, size, folder) <>((DataTrack.fromValues _).tupled, (t: DataTrack) => t.toValues)
}

class Folders(tag: Tag) extends Table[DataFolder](tag, "FOLDERS") {
  def id = column[FolderID]("ID", O.PrimaryKey)

  def title = column[String]("TITLE")

  def path = column[PimpPath]("PATH")

  def parent = column[FolderID]("PARENT")

  // foreign key to itself; the root folder is its own parent
  def parentFolder = foreignKey("PARENT_FK", parent, PimpSchema.folders)(
    _.id,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade)

  def * = (id, title, path, parent) <>((DataFolder.apply _).tupled, DataFolder.unapply)
}

/**
  * Temp tables.
  */
class TempFolders(tag: Tag) extends Table[TempFolder](tag, "TEMP_FOLDERS") {
  def id = column[FolderID]("ID", O.PrimaryKey)

  def * = id <>(TempFolder.apply, TempFolder.unapply)
}

case class TempFolder(id: FolderID)

class TempTracks(tag: Tag) extends Table[TempTrack](tag, "TEMP_TRACKS") {
  def id = column[TrackID]("ID", O.PrimaryKey)

  def * = id <>(TempTrack.apply, TempTrack.unapply)
}

case class TempTrack(id: TrackID)
