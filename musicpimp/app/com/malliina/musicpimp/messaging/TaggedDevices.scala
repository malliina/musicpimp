package com.malliina.musicpimp.messaging

import com.malliina.io.FileSet
import io.circe.Codec

class TaggedDevices[T <: TaggedDevice[?]](file: String)(implicit format: Codec[T])
  extends FileSet[T](file):

  override protected def id(elem: T): String = elem.tag.tag

  def removeWhere(p: T => Boolean): Seq[T] =
    val (goes, stays) = load().partition(p)
    persist(stays)
    goes
