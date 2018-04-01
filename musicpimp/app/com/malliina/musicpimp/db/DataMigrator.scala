package com.malliina.musicpimp.db

import java.nio.file.{Files, Path, StandardOpenOption}

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.file.FileUtilities
import com.malliina.musicpimp.audio.PimpEnc
import com.malliina.musicpimp.auth.DataUser
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.musicpimp.util.FileUtil
import com.malliina.play.auth.Token
import com.malliina.values.UnixPath
import play.api.Logger
import play.api.libs.json.{Json, OFormat}

import scala.concurrent.Future
import scala.util.Try

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

object DataMigrator {
  def apply(db: PimpDb) = new DataMigrator(db)
}

class DataMigrator(db: PimpDb) {
  private val log = Logger(getClass)

  import db.api._
  import db.schema._
  import db.schema.mappings._

  import concurrent.duration.DurationInt

  val versionFile = FileUtil.localPath("db-version.txt")
  val desiredVersion = 1

  def saveVersion(v: Int) = FileUtilities.stringToFile(v.toString, versionFile)

  def loadVersion(): Int = Try(FileUtilities.fileToString(versionFile).toInt) getOrElse 0

  def migrateDatabase() = {
    if (loadVersion() < desiredVersion) {
      log.info("Performing migration...")
      alterTable()
      updateFolderIds()
      updateTrackIds()
      recreateIndex()
      saveVersion(desiredVersion)
      log.info(s"Migration to version $desiredVersion complete.")
    } else {
      log.info("Schema up to date.")
    }
  }

  def alterTable() = {
    val a = sqlu"""ALTER TABLE TRACKS ADD PATH VARCHAR(2048) NOT NULL DEFAULT '';"""
    val result = db.run(a).recover { case e: Exception => log.debug("SQL error", e) }
    await(result)
    log.info("Schema migration complete.")
  }

  def updateFolderIds() = {
    val newFolderIds = folders.result.flatMap { fs =>
      val ops = fs.map { folder =>
        val newId = FolderID(Library.idFor(folder.path.path))
        folders.filter(f => f.id === folder.id).map(_.id).update(newId)
      }
      DBIO.sequence(ops)
    }
    val result = db.run(newFolderIds.transactionally)
    await(result)
    log.info("Folder ID update complete.")
  }

  def updateTrackIds() = {
    val newIds = tracks.result.flatMap { ts =>
      val ops = ts.map { track =>
        val src = UnixPath.fromRaw(PimpEnc.decodeId(track.id))
        val newId = TrackID(Library.idFor(src.path))
        val row = tracks.filter(t => t.id === track.id && t.path === UnixPath.Empty)
        DBIO.seq(
          row.map(_.path).update(src),
          row.map(_.id).update(newId)
        )
      }
      DBIO.sequence(ops)
    }
    val result = db.run(newIds.transactionally)
    await(result)
    log.info("Track ID update complete.")
  }

  def recreateIndex() = {
    log.info("Recreating index...")
    await(db.recreateIndex())
    log.info("Index recreated.")
  }

  def await[T](f: Future[T]) = db.await(f, 7200.seconds)

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
