package controllers

import java.net.ConnectException

import com.mle.concurrent.FutureOps
import com.mle.musicpimp.cloud.Clouds
import com.mle.play.Authenticator
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Result
import views.html

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author Michael
 */
class Cloud(clouds: Clouds, auth: Authenticator) extends Secured(auth) {
  val idFormKey = "id"
  val FEEDBACK = "feedback"
  val cloudForm = Form(idFormKey -> optional(text))

  def cloud = PimpActionAsync(implicit req => {
    val id = clouds.registration.map(id => (Some(id), None)).recoverAll(t => (None, Some(t.getMessage)))
    id map (i => Ok(views.html.cloud(this, cloudForm, i._1, i._2)))
  })

  def toggle = PimpActionAsync(implicit req => {
    cloudForm.bindFromRequest.fold(
      formErrors => {
        log debug s"Form errors: $formErrors"
        Future successful BadRequest(html.cloud(this, formErrors))
      },
      desiredID => {
        val redir = Redirect(routes.Cloud.cloud())
        val maybeID = desiredID.filter(_.nonEmpty)
        if (clouds.client.isConnected) {
          clouds.disconnectAndForget()
          Future(redir)
        } else {
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
