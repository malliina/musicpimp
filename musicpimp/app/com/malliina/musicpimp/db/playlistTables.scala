package com.malliina.musicpimp.db

import com.malliina.musicpimp.db.Mappings.username
import com.malliina.musicpimp.models.TrackID
import com.malliina.play.models.Username
import slick.jdbc.H2Profile.api._

class PlaylistTable(tag: Tag) extends Table[PlaylistRow](tag, "PLAYLISTS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  def name = column[String]("NAME")

  def user = column[Username]("USER")

  def userConstraint = foreignKey("USER_FK", user, PimpSchema.usersTable)(
    _.user,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade)

  def * = (id.?, name, user) <>(build, unbuild)

  def build(kvs: (Option[Long], String, Username)): PlaylistRow = kvs match {
    case (i, n, u) => PlaylistRow(i, n, u)
  }

  def unbuild(row: PlaylistRow): Option[(Option[Long], String, Username)] = {
    Option((row.id, row.name, row.user))
  }
}

case class PlaylistRow(id: Option[Long], name: String, user: Username)

class PlaylistTracks(tag: Tag) extends Table[PlaylistTrack](tag, "PLAYLIST_TRACKS") {
  def playlist = column[Long]("PLAYLIST")

  def track = column[TrackID]("TRACK")

  def idx = column[Int]("INDEX")

  def pk = primaryKey("PT_PK", (playlist, idx))

  def playlistConstraint = foreignKey("PLAYLIST_FK", playlist, PimpSchema.playlistsTable)(
    _.id,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade)

  def trackConstraint = foreignKey("PL_TRACK_FK", track, PimpSchema.tracks)(
    _.id,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade)

  def * = (playlist, track, idx) <>((PlaylistTrack.apply _).tupled, PlaylistTrack.unapply)
}

case class PlaylistTrack(id: Long, track: TrackID, index: Int)
