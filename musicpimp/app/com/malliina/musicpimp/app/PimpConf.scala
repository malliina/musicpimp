package com.malliina.musicpimp.app

import java.nio.file.Paths

import com.malliina.musicpimp.util.FileUtil
import com.malliina.values.ErrorMessage

object PimpConf {
  val pimpConfFile = FileUtil.localPath("musicpimp.conf")
  val homeConf = Paths.get(sys.props("user.home"), ".musicpimp", "musicpimp.conf")
  val fileProps: Map[String, String] = FileUtil.props(pimpConfFile) ++ FileUtil.props(homeConf)

  def readConfFile(key: String): Option[String] = fileProps.get(key)

  def read(key: String) =
    sys.env.get(key).orElse(sys.props.get(key)).orElse(readConfFile(key)).toRight(ErrorMessage(s"Key missing: '$key'."))
}
