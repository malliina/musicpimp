package com.malliina.musicpimp.db

import java.time.Instant

import com.malliina.musicpimp.auth.DataUser
import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.play.auth.Token
import com.malliina.play.models.Username
import com.malliina.values.UnixPath
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

object PimpSchema {
  def apply(profile: JdbcProfile) = new PimpSchema(profile)
}

class PimpSchema(val profile: JdbcProfile) {
  val mappings = Mappings(profile)
  val userMapping = mappings.username
  val trackMapping = mappings.trackId
  val instantMapping = mappings.instant

  import mappings._
  import profile.api._

  val tracks = TableQuery[Tracks]
  val folders = TableQuery[Folders]
  val tokens = TableQuery[TokensTable]
  val usersTable = TableQuery[Users]
  val tempFoldersTable = TableQuery[TempFolders]
  val tempTracksTable = TableQuery[TempTracks]
  val playlistsTable = TableQuery[PlaylistTable]
  val playlistTracksTable = TableQuery[PlaylistTracks]
  val plays = TableQuery[Plays]

  val tableQueries: Seq[TableQuery[_ <: Table[_]]] = Seq(
    plays, playlistTracksTable, playlistsTable,
    tempFoldersTable, tempTracksTable, tracks,
    folders, tokens, usersTable
  )

  class TokensTable(tag: Tag) extends Table[Token](tag, "TOKENS") {
    def user = column[Username]("USER")

    def series = column[Long]("SERIES")

    def token = column[Long]("TOKEN")

    def * = (user, series, token) <> ((TokensTable.fromRaw _).tupled, TokensTable.write)
  }

  object TokensTable {
    def fromRaw(user: Username, series: Long, token: Long): Token =
      Token(user, series, token)

    def write(t: Token): Option[(Username, Long, Long)] =
      Option((t.user, t.series, t.token))
  }

  class PlaylistTable(tag: Tag) extends Table[PlaylistRow](tag, "PLAYLISTS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def name = column[String]("NAME")

    def user = column[Username]("USER", O.Length(100))

    def userConstraint = foreignKey("USER_FK", user, usersTable)(
      _.user,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, name, user) <> (build, unbuild)

    def build(kvs: (Option[Long], String, Username)): PlaylistRow = kvs match {
      case (i, n, u) => PlaylistRow(i, n, u)
    }

    def unbuild(row: PlaylistRow): Option[(Option[Long], String, Username)] = {
      Option((row.id, row.name, row.user))
    }
  }

  class PlaylistTracks(tag: Tag) extends Table[PlaylistTrack](tag, "PLAYLIST_TRACKS") {
    def playlist = column[Long]("PLAYLIST")

    def track = column[TrackID]("TRACK", O.Length(191))

    def idx = column[Int]("INDEX")

    def pk = primaryKey("PT_PK", (playlist, idx))

    def playlistConstraint = foreignKey("PLAYLIST_FK", playlist, playlistsTable)(
      _.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Cascade)

    def trackConstraint = foreignKey("PL_TRACK_FK", track, tracks)(
      _.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Cascade)

    def * = (playlist, track, idx) <> ((PlaylistTrack.apply _).tupled, PlaylistTrack.unapply)
  }

  class Plays(tag: Tag) extends Table[PlaybackRecord](tag, "PLAYS") {
    def track = column[TrackID]("TRACK", O.Length(191))

    def when = column[Instant]("WHEN")

    def who = column[Username]("WHO", O.Length(100))

    def trackConstraint = foreignKey("TRACK_FK", track, tracks)(
      _.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.NoAction
    )

    def whoConstraint = foreignKey("WHO_FK", who, usersTable)(
      _.user,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Cascade
    )

    def * : ProvenShape[PlaybackRecord] =
      (track, when, who) <> ((PlaybackRecord.apply _).tupled, PlaybackRecord.unapply)
  }

  class Tracks(tag: Tag) extends Table[DataTrack](tag, "TRACKS") {
    def id = column[TrackID]("ID", O.PrimaryKey, O.Length(191))

    def title = column[String]("TITLE")

    def artist = column[String]("ARTIST")

    def album = column[String]("ALBUM")

    def duration = column[Int]("DURATION")

    def size = column[Long]("SIZE")

    def folder = column[FolderID]("FOLDER", O.Length(191))

    def folderConstraint = foreignKey("FOLDER_FK", folder, folders)(
      _.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Cascade)

    def * = (id, title, artist, album, duration, size, folder) <> ((DataTrack.fromValues _).tupled, (t: DataTrack) => t.toValues)
  }

  class Folders(tag: Tag) extends Table[DataFolder](tag, "FOLDERS") {
    def id = column[FolderID]("ID", O.PrimaryKey, O.Length(191))

    def title = column[String]("TITLE")

    def path = column[UnixPath]("PATH")

    def parent = column[FolderID]("PARENT", O.Length(191))

    // foreign key to itself; the root folder is its own parent
    def parentFolder = foreignKey("PARENT_FK", parent, folders)(
      _.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Cascade)

    def * = (id, title, path, parent) <> ((DataFolder.apply _).tupled, DataFolder.unapply)
  }

  /**
    * Temp tables.
    */
  class TempFolders(tag: Tag) extends Table[TempFolder](tag, "TEMP_FOLDERS") {
    def id = column[FolderID]("ID", O.PrimaryKey, O.Length(191))

    def * = id <> (TempFolder.apply, TempFolder.unapply)
  }


  class TempTracks(tag: Tag) extends Table[TempTrack](tag, "TEMP_TRACKS") {
    def id = column[TrackID]("ID", O.PrimaryKey, O.Length(191))

    def * = id <> (TempTrack.apply, TempTrack.unapply)
  }


  class Users(tag: Tag) extends Table[DataUser](tag, "USERS") {
    def user = column[Username]("USER", O.PrimaryKey, O.Length(100))

    def passHash = column[String]("PASS_HASH")

    def * = (user, passHash) <> ((DataUser.apply _).tupled, DataUser.unapply)
  }

}

case class PlaylistRow(id: Option[Long], name: String, user: Username)

object PlaylistRow {
  implicit val json = Json.format[PlaylistRow]
}

case class PlaylistTrack(id: Long, track: TrackID, index: Int)

object PlaylistTrack {
  implicit val json = Json.format[PlaylistTrack]
}

case class PlaybackRecord(track: TrackID, when: Instant, user: Username)

object PlaybackRecord {
  implicit val json: OFormat[PlaybackRecord] = Json.format[PlaybackRecord]
}

case class TempFolder(id: FolderID)

case class TempTrack(id: TrackID)
