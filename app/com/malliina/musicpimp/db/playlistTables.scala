package com.malliina.musicpimp.db

import com.malliina.musicpimp.models.User
import slick.driver.H2Driver.api._

class PlaylistTable(tag: Tag) extends Table[PlaylistRow](tag, "PLAYLISTS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  def name = column[String]("NAME")

  def user = column[User]("USER")

  def userConstraint = foreignKey("USER_FK", user, PimpSchema.usersTable)(
    _.user,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade)

  def * = (id.?, name, user) <>(build, unbuild)

  def build(kvs: (Option[Long], String, User)): PlaylistRow = kvs match {
    case (i, n, u) => PlaylistRow(i, n, u)
  }

  def unbuild(row: PlaylistRow): Option[(Option[Long], String, User)] = {
    Option((row.id, row.name, row.user))
  }
}

case class PlaylistRow(id: Option[Long], name: String, user: User)

class PlaylistTracks(tag: Tag) extends Table[PlaylistTrack](tag, "PLAYLIST_TRACKS") {
  def playlist = column[Long]("PLAYLIST")

  def track = column[String]("TRACK")

  def idx = column[Int]("INDEX")

  def pk = primaryKey("PT_PK", (playlist, idx))

  def playlistConstraint = foreignKey("PLAYLIST_FK", playlist, PimpSchema.playlistsTable)(
    _.id,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade)

  def trackConstraint = foreignKey("TRACK_FK", track, PimpSchema.tracks)(
    _.id,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade)

  def * = (playlist, track, idx) <>((PlaylistTrack.apply _).tupled, PlaylistTrack.unapply)
}

case class PlaylistTrack(id: Long, track: String, index: Int)