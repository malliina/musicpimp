package controllers.musicpimp

import akka.stream.Materializer
import com.malliina.play.controllers.AuthBundle
import com.malliina.play.http.AuthedRequest

case class AuthDeps(auth: AuthBundle[AuthedRequest], mat: Materializer) {
  val ec = mat.executionContext
}
