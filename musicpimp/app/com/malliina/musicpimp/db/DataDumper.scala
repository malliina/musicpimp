package com.malliina.musicpimp.db

import java.nio.file.{Files, Path, StandardOpenOption}

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.musicpimp.auth.DataUser
import com.malliina.play.auth.Token
import play.api.Logger
import play.api.libs.json.{Json, OFormat}

import scala.concurrent.Future

case class DataDump(users: Seq[DataUser],
                    folders: Seq[DataFolder],
                    tracks: Seq[DataTrack],
                    plays: Seq[PlaybackRecord],
                    playlists: Seq[PlaylistRow],
                    playlistTracks: Seq[PlaylistTrack],
                    tokens: Seq[Token])

object DataDump {
  implicit val json: OFormat[DataDump] = Json.format[DataDump]
}

object DataDumper {
  def apply(db: PimpDb) = new DataDumper(db)
}

class DataDumper(db: PimpDb) {
  private val log = Logger(getClass)

  import db.api._
  import db.schema._

  def writeDump(to: Path): Future[Unit] = {
    dump().map { data =>
      Files.write(to, Json.toBytes(Json.toJson(data)), StandardOpenOption.CREATE)
      log.info(s"Wrote dump to '$to'.")
    }
  }

  def dump(): Future[DataDump] = {
    val action = for {
      us <- usersTable.result
      fs <- folders.result
      ts <- tracks.result
      ps <- plays.result
      pls <- playlistsTable.result
      plts <- playlistTracksTable.result
      toks <- tokens.result
    } yield {
      DataDump(us, fs, ts, ps, pls, plts, toks)
    }
    db.run(action.transactionally)
  }

  def restoreDump(from: Path, fromScratch: Boolean = false): Future[Unit] = {
    log.info(s"Restoring dump from '$from'...")
    val dump = Json.parse(Files.readAllBytes(from)).as[DataDump]
    val action = for {
      _ <- usersTable ++= dump.users
      _ <- folders ++= dump.folders
      _ <- tracks ++= dump.tracks
      _ <- plays ++= dump.plays
      _ <- playlistsTable ++= dump.playlists
      _ <- playlistTracksTable ++= dump.playlistTracks
      _ <- tokens ++= dump.tokens
    } yield ()
    if (fromScratch) deleteAll().flatMap(_ => db.run(action.transactionally))
    else db.run(action.transactionally)
  }

  def deleteAll() = {
    val action = for {
      _ <- usersTable.delete
      _ <- folders.delete
      _ <- tracks.delete
      _ <- plays.delete
      _ <- playlistsTable.delete
      _ <- playlistTracksTable.delete
      _ <- tokens.delete
    } yield ()
    db.run(action)
  }
}
