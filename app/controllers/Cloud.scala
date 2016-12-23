package controllers

import java.net.ConnectException

import akka.stream.Materializer
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.cloud.{CloudID, Clouds}
import com.malliina.musicpimp.tags.PimpTags
import com.malliina.play.Authenticator
import controllers.Cloud.log
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import scala.concurrent.Future

class Cloud(tags: PimpTags, clouds: Clouds, auth: Authenticator, mat: Materializer) extends Secured(auth, mat) {
  val idFormKey = "id"
  val FEEDBACK = "feedback"
  val cloudForm = Form(idFormKey -> optional(text))

  def cloud = pimpActionAsync { request =>
    val id = clouds
      .registration.map(id => (Some(id), None))
      .recoverAll(t => (None, Some(t.getMessage)))
    val formFeedback = UserFeedback.flashed(request)
    id.map({ case (cloudId, errorMessage) =>
      Ok(tags.cloud(this, cloudId.map(_.id), errorMessage, request.user, formFeedback))
    })
  }

  def toggle = pimpParsedActionAsync(parse.default) { request =>
    cloudForm.bindFromRequest()(request).fold(
      formErrors => {
        log debug s"Form errors: $formErrors"
        val feedback = UserFeedback.formed(formErrors) orElse UserFeedback.flashed(request)
        Future successful BadRequest(tags.cloud(this, None, None, request.user, feedback))
      },
      desiredID => {
        val redir = Redirect(routes.Cloud.cloud())
        if (clouds.client.isConnected) {
          clouds.disconnectAndForget()
          fut(redir)
        } else {
          val maybeID = desiredID.filter(_.nonEmpty).map(CloudID.apply)
          clouds
            .connect(maybeID).map(_ => redir)
            .recover(errorMessage andThen (msg => redir.flashing(
              UserFeedback.Feedback -> msg,
              UserFeedback.Success -> UserFeedback.No)))
        }
      }
    )
  }

  def errorMessage: PartialFunction[Throwable, String] = {
    case ce: ConnectException =>
      "Unable to connect to the cloud. Please try again later."
    case t: Throwable =>
      val msg = "Unable to connect to the cloud."
      log.error(msg, t)
      msg
  }
}

object Cloud {
  private val log = Logger(getClass)
}
