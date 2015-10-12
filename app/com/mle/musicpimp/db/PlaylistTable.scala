package com.mle.musicpimp.db

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

  def * = (id, name, user) <>((PlaylistRow.apply _).tupled, PlaylistRow.unapply)
}

case class PlaylistRow(id: Long, name: String, user: String)

class PlaylistTracks(tag: Tag) extends Table[PlaylistTrack](tag, "PLAYLIST_TRACKS") {
  def playlist = column[Long]("PLAYLIST", O.NotNull)

  def track = column[String]("TRACK", O.NotNull)

  def playlistConstraint = foreignKey("PLAYLIST_FK", playlist, PimpDb.playlistsTable)(_.id,
    onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

  def trackConstraint = foreignKey("TRACK_FK", track, PimpDb.tracks)(_.id,
    onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

  def * = (playlist, track) <>((PlaylistTrack.apply _).tupled, PlaylistTrack.unapply)
}

case class PlaylistTrack(id: Long, track: String)
