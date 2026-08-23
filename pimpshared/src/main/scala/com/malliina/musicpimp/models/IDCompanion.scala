package com.malliina.musicpimp.models

import com.malliina.values.ValidatingCompanion

import scala.reflect.ClassTag

abstract class IDCompanion[T <: Identifier: ClassTag] extends ValidatingCompanion[String, T]:
  override def write(t: T): String = t.id
