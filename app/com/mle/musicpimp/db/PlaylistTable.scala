package com.mle.musicpimp.db

import com.mle.musicpimp.models.User

import scala.slick.driver.H2Driver.simple._

/**
 * @author mle
 */
class PlaylistTable(tag: Tag) extends Table[PlaylistRow](tag, "PLAYLISTS") {
  def id = column[Long]("ID", O.PrimaryKey, O.NotNull)

  def name = column[String]("NAME", O.NotNull)

  def user = column[String]("USER", O.NotNull)

  def userConstraint = foreignKey("USER_FK", user, PimpDb.usersTable)(_.user,
    onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

  def * = (id.?, name, user) <>(build, unbuild)

  def build(kvs: (Option[Long], String, String)): PlaylistRow = kvs match {
    case (i, n, u) => PlaylistRow(i, n, User(u))
  }

  def unbuild(row: PlaylistRow): Option[(Option[Long], String, String)] = {
    Option((row.id, row.name, row.user.name))
  }
}

case class PlaylistRow(id: Option[Long], name: String, user: User)

class PlaylistTracks(tag: Tag) extends Table[PlaylistTrack](tag, "PLAYLIST_TRACKS") {
  def playlist = column[Long]("PLAYLIST", O.NotNull)

  def track = column[String]("TRACK", O.NotNull)

  def idx = column[Int]("INDEX", O.NotNull)

  def pk = primaryKey("PT_PK", (playlist, idx))

  def playlistConstraint = foreignKey("PLAYLIST_FK", playlist, PimpDb.playlistsTable)(_.id,
    onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

  def trackConstraint = foreignKey("TRACK_FK", track, PimpDb.tracks)(_.id,
    onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

  def * = (playlist, track, idx) <>((PlaylistTrack.apply _).tupled, PlaylistTrack.unapply)
}

case class PlaylistTrack(id: Long, track: String, index: Int)
