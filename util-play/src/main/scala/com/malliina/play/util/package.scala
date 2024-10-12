package com.malliina.play

import com.malliina.values.ErrorMessage
import play.api.Configuration

import scala.util.Try

package object util {

  implicit class ConfOps(conf: Configuration) {
    def read(key: String): Either[ErrorMessage, String] =
      Try(conf.get[String](key)).toOption.toRight(ErrorMessage(s"Missing key: '$key'."))
  }

}
