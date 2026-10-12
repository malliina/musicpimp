package com.malliina.musicpimp.db

import com.malliina.auth.Token
import com.malliina.musicpimp.models.{FolderID, PlaylistID, TrackID}
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.values.{ErrorMessage, UnixPath, Username, ValidatingCompanion}
import doobie.{Meta, Read}

import java.time.Instant
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object DoobieMappings extends DoobieMappings

trait DoobieMappings:
  given instantMeta: Meta[Instant] = doobie.implicits.legacy.instant.JavaTimeInstantMeta
  given Meta[Username] = Meta[String].timap(Username.build(_).getUnsafe)(_.name)
  given Meta[TrackID] = meta(TrackID)
  given playlistIdMeta: Meta[PlaylistID] = meta(PlaylistID)
  given Meta[FolderID] = meta(FolderID)
  given Meta[UnixPath] = meta(UnixPath)
  given finiteDurationMeta: Meta[FiniteDuration] = Meta[Int].timap(_.seconds)(_.toSeconds.toInt)
  given Meta[StorageSize] = Meta[Long].timap(_.bytes)(_.toBytes)

  given Read[Token] = Read.derived

  def meta[T, R: Meta, C <: ValidatingCompanion[R, T]](c: C): Meta[T] =
    Meta[R].timap(c.build(_).getUnsafe)(c.write)

extension [T](e: Either[ErrorMessage, T])
  def getUnsafe: T = e.fold(err => throw IllegalArgumentException(err.message), identity)
