package com.malliina.musicpimp.db

import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.values.{ErrorMessage, UnixPath, Username, ValidatingCompanion}
import doobie.Meta

import java.time.Instant
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait DoobieMappings:
  given Meta[Instant] = doobie.implicits.legacy.instant.JavaTimeInstantMeta
  given Meta[Username] = Meta[String].timap(Username.build(_).getUnsafe)(_.name)
  given Meta[TrackID] = meta(TrackID)
  given Meta[FolderID] = meta(FolderID)
  given Meta[UnixPath] = meta(UnixPath)
  given Meta[FiniteDuration] = Meta[Int].timap(_.seconds)(_.toSeconds.toInt)
  given Meta[StorageSize] = Meta[Long].timap(_.bytes)(_.toBytes)

  def meta[T, R: Meta, C <: ValidatingCompanion[R, T]](c: C): Meta[T] =
    Meta[R].timap(c.build(_).getUnsafe)(c.write)

extension [T](e: Either[ErrorMessage, T])
  def getUnsafe: T = e.fold(err => throw IllegalArgumentException(err.message), identity)
