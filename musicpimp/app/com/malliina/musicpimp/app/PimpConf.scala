package com.malliina.musicpimp.app

import com.malliina.musicpimp.util.FileUtil
import com.malliina.values.ErrorMessage

object PimpConf {
  val pimpConfFile = FileUtil.localPath("musicpimp.conf")
  val fileProps = FileUtil.props(pimpConfFile)

  def readConfFile(key: String): Option[String] = fileProps.get(key)

  def read(key: String) =
    sys.env.get(key).orElse(sys.props.get(key)).orElse(readConfFile(key)).toRight(ErrorMessage(s"Key missing: '$key'."))
}
