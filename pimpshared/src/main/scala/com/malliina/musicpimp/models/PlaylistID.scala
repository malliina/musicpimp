package com.malliina.musicpimp.models

import com.malliina.values.{ErrorMessage, ValidatingCompanion}

case class PlaylistID(id: Long) extends AnyVal:
  override def toString = s"$id"

object PlaylistID extends ValidatingCompanion[Long, PlaylistID]:
  override def build(input: Long): Either[ErrorMessage, PlaylistID] = Right(apply(input))

  override def write(t: PlaylistID): Long = t.id

  def unapply(in: String): Option[PlaylistID] =
    in.toLongOption.flatMap(l => build(l).toOption)
