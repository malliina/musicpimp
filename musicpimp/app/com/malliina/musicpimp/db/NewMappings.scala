package com.malliina.musicpimp.db

import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.values.UnixPath
import io.getquill.MappedEncoding

import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait NewMappings:
  implicit val durationDecoder: MappedEncoding[Int, FiniteDuration] =
    MappedEncoding[Int, FiniteDuration](_.seconds)
  implicit val durationEncoder: MappedEncoding[FiniteDuration, Int] =
    MappedEncoding[FiniteDuration, Int](_.toSeconds.toInt)

  implicit val storageDecoder: MappedEncoding[Long, StorageSize] =
    MappedEncoding[Long, StorageSize](_.bytes)
  implicit val storageEncoder: MappedEncoding[StorageSize, Long] =
    MappedEncoding[StorageSize, Long](_.toBytes)

  implicit val unixPathDecoder: MappedEncoding[String, UnixPath] =
    MappedEncoding[String, UnixPath](UnixPath.apply)
  implicit val unixPathEncoder: MappedEncoding[UnixPath, String] =
    MappedEncoding[UnixPath, String](_.path)
