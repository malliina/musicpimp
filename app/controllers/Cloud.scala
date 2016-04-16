package controllers

import java.net.ConnectException

import akka.stream.Materializer
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.cloud.{CloudID, Clouds}
import com.malliina.play.Authenticator
import controllers.Cloud.log
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import views.html

import scala.concurrent.Future

class Cloud(clouds: Clouds, auth: Authenticator, mat: Materializer) extends Secured(auth, mat) {
  val idFormKey = "id"
  val FEEDBACK = "feedback"
  val cloudForm = Form(idFormKey -> optional(text))

  def cloud = PimpActionAsync(implicit req => {
    val id = clouds.registration.map(id => (Some(id), None)).recoverAll(t => (None, Some(t.getMessage)))
    id map (i => Ok(html.cloud(this, cloudForm, i._1.map(_.id), i._2)))
  })

  def toggle = PimpParsedActionAsync(parse.default)(implicit req => {
    cloudForm.bindFromRequest.fold(
      formErrors => {
        log debug s"Form errors: $formErrors"
        Future successful BadRequest(html.cloud(this, formErrors))
      },
      desiredID => {
        val redir = Redirect(routes.Cloud.cloud())
        if (clouds.client.isConnected) {
          clouds.disconnectAndForget()
          Future.successful(redir)
        } else {
          val maybeID = desiredID.filter(_.nonEmpty).map(CloudID.apply)
          clouds.connect(maybeID).map(_ => redir).recover(errorMessage andThen (msg => redir.flashing(FEEDBACK -> msg)))
        }
      }
    )
  })

  def errorMessage: PartialFunction[Throwable, String] = {
    case ce: ConnectException =>
      "Unable to connect to the cloud. Please try again later."
    case t: Throwable =>
      val msg = "Unable to connect to the cloud."
      log.error(msg, t)
      msg
  }

  def withError(result: Result, message: String) = result.flashing(FEEDBACK -> message)
}

object Cloud {
  private val log = Logger(getClass)
}
