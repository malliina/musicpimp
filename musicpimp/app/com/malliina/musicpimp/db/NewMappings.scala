package com.malliina.musicpimp.db

import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.values.UnixPath
import io.getquill.MappedEncoding

import scala.concurrent.duration.{DurationLong, FiniteDuration}

trait NewMappings {
  implicit val durationDecoder = MappedEncoding[Int, FiniteDuration](_.seconds)
  implicit val durationEncoder = MappedEncoding[FiniteDuration, Int](_.toSeconds.toInt)

  implicit val storageDecoder = MappedEncoding[Long, StorageSize](_.bytes)
  implicit val storageEncoder = MappedEncoding[StorageSize, Long](_.toBytes)

  implicit val unixPathDecoder = MappedEncoding[String, UnixPath](UnixPath.apply)
  implicit val unixPathEncoder = MappedEncoding[UnixPath, String](_.path)
}
