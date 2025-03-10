package controllers.musicpimp

import org.apache.pekko.stream.Materializer
import com.malliina.play.controllers.AuthBundle
import com.malliina.play.http.AuthedRequest
import play.api.mvc.ControllerComponents

case class AuthDeps(
  comps: ControllerComponents,
  auth: AuthBundle[AuthedRequest],
  mat: Materializer
)
