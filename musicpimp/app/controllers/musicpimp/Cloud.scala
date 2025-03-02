package controllers.musicpimp

import java.net.ConnectException

import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.models.CloudID
import com.malliina.musicpimp.html.PimpHtml
import controllers.musicpimp.Cloud.log
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.*

import scala.concurrent.Future

object Cloud:
  private val log = Logger(getClass)
  val idFormKey = "id"

class Cloud(tags: PimpHtml, clouds: Clouds, auth: AuthDeps) extends Secured(auth):

  val FEEDBACK = "feedback"
  val cloudForm = Form(Cloud.idFormKey -> optional(text))

  def cloud = pimpActionAsync: request =>
    val id = clouds.registration
      .map[(Option[CloudID], Option[UserFeedback])](id => (Some(id), None))
      .recoverAll: t =>
        log.warn(s"Cloud connection failed.", t)
        (None, Option(UserFeedback.error(t.getMessage)))
    id.map:
      case (cloudId, errorMessage) =>
        val feedback = UserFeedback.flashed(request) orElse errorMessage
        Ok(tags.cloud(cloudId, feedback, request.user))

  def toggle = pimpParsedActionAsync(parsers.default): request =>
    cloudForm
      .bindFromRequest()(request, formBinding)
      .fold(
        formErrors =>
          log debug s"Form errors: $formErrors"
          val feedback = UserFeedback.formed(formErrors) orElse UserFeedback.flashed(request)
          Future successful BadRequest(tags.cloud(None, feedback, request.user))
        ,
        desiredID =>
          val redir = Redirect(routes.Cloud.cloud)
          if clouds.isConnected then
            clouds.disconnectAndForget("Disconnected by request.")
            fut(redir)
          else
            val maybeID = desiredID.filter(_.nonEmpty).map(CloudID.apply)
            clouds
              .connect(maybeID)
              .map(_ => redir)
              .recover(
                errorMessage andThen (msg =>
                  redir.flashing(
                    UserFeedback.Feedback -> msg,
                    UserFeedback.Success -> UserFeedback.No
                  )
                )
              )
      )

  def errorMessage: PartialFunction[Throwable, String] = {
    case ce: ConnectException =>
      log.error(s"Unable to connect to ${clouds.uri}.", ce)
      "Unable to connect to the cloud. Please try again later."
    case t: Throwable =>
      val msg = "Unable to connect to the cloud."
      log.error(msg, t)
      msg
  }
