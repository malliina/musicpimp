package controllers.pimpcloud

import akka.stream.Materializer
import com.malliina.play.controllers.BaseSecurity
import controllers.pimpcloud.CloudAuth.{IntendedUri, log}
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc.{Call, RequestHeader, Result}

class CloudAuth(mat: Materializer) extends BaseSecurity(mat) {
  protected override def onUnauthorized(request: RequestHeader): Result = {
    logUnauthorized(request)
    log debug s"Intended: ${request.uri}"
    PimpContentController.pimpResult(request)(
      html = Redirect(loginRedirectCall).withSession(IntendedUri -> request.uri),
      json = Unauthorized
    )
  }

  def loginRedirectCall: Call = routes.Web.login()

  protected def logUnauthorized(request: RequestHeader): Unit =
    log warn s"Unauthorized request '${request.path}' from '${request.remoteAddress}'."
}

object CloudAuth {
  private val log = Logger(getClass)
  val IntendedUri = "intended_uri"
}
