package com.malliina.musicpimp.util

import com.malliina.values.{ErrorMessage, Readable}

object Sys:
  val env = Sys(sys.env)

class Sys(src: Map[String, String]):
  def readOpt[R](key: String)(using r: Readable[R]): Option[R] =
    read[R](key).fold(_ => None, identity)

  def read[R](key: String)(using r: Readable[R]): Either[ErrorMessage, Option[R]] =
    src.get(key).map(s => r.read(s).map(r => Option(r))).getOrElse(Right(None))
