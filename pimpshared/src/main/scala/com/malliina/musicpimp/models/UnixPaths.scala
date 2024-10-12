package com.malliina.musicpimp.models

import com.malliina.play.PlayString
import com.malliina.values.UnixPath

object UnixPaths extends PlayString[UnixPath]:
  override def apply(raw: String) = UnixPath(raw)

  override def raw(t: UnixPath) = t.path
