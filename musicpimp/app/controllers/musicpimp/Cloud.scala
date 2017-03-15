package controllers.musicpimp

import java.net.ConnectException

import akka.stream.Materializer
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.models.CloudID
import com.malliina.musicpimp.tags.PimpHtml
import com.malliina.play.CookieAuthenticator
import controllers.musicpimp.Cloud.log
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent.Future

object Cloud {
  private val log = Logger(getClass)
  val idFormKey = "id"
}

class Cloud(tags: PimpHtml, clouds: Clouds, auth: CookieAuthenticator, mat: Materializer)
  extends Secured(auth, mat) {

  val FEEDBACK = "feedback"
  val cloudForm = Form(Cloud.idFormKey -> optional(text))

  def cloud = pimpActionAsync { request =>
    val id = clouds
      .registration.map[(Option[CloudID], Option[UserFeedback])](id => (Some(id), None))
      .recoverAll(t => (None, Option(UserFeedback.error(t.getMessage))))
    id.map { case (cloudId, errorMessage) =>
      val feedback = UserFeedback.flashed(request) orElse errorMessage
      Ok(tags.cloud(cloudId, feedback, request.user))
    }
  }

  def toggle = pimpParsedActionAsync(parse.default) { request =>
    cloudForm.bindFromRequest()(request).fold(
      formErrors => {
        log debug s"Form errors: $formErrors"
        val feedback = UserFeedback.formed(formErrors) orElse UserFeedback.flashed(request)
        Future successful BadRequest(tags.cloud(None, feedback, request.user))
      },
      desiredID => {
        val redir = Redirect(routes.Cloud.cloud())
        if (clouds.isConnected) {
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
      log.error(s"Unable to connect to ${clouds.uri}.", ce)
      "Unable to connect to the cloud. Please try again later."
    case t: Throwable =>
      val msg = "Unable to connect to the cloud."
      log.error(msg, t)
      msg
  }
}
