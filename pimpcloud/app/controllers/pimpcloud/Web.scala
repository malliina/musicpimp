package controllers.pimpcloud

import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.auth.{CloudAuthentication, CloudCredentials}
import com.malliina.play.controllers.{AccountForms, Caching}
import com.malliina.play.models.{Password, Username}
import controllers.pimpcloud.Web.log
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class CloudCreds(cloudID: CloudID, username: Username, pass: Password)

class Web(tags: CloudTags,
          authActions: CloudAuthentication,
          exec: ExecutionContext,
          val forms: AccountForms)
  extends Controller {

  val serverFormKey = "server"
  val cloudForm = Form[CloudCreds](mapping(
    serverFormKey -> CloudID.mapping,
    forms.userFormKey -> Username.mapping,
    forms.passFormKey -> Password.mapping
  )(CloudCreds.apply)(CloudCreds.unapply))

  def ping = Action {
    Caching.NoCache(Ok)
  }

  def login = Action { req =>
    Ok(loginPage(cloudForm, req.flash))
  }

  def formAuthenticate = Action.async { request =>
    val flash = request.flash
    val remoteAddress = request.remoteAddress
    cloudForm.bindFromRequest()(request).fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(forms.userFormKey, "")
        log warn s"Authentication failed for user: $user from: $remoteAddress"
        fut(BadRequest(loginPage(formWithErrors, flash)))
      },
      cloudCreds => {
        implicit val ec = exec
        val creds = CloudCredentials(cloudCreds.cloudID, cloudCreds.username, cloudCreds.pass, request)
        authActions.validate(creds).map(_ => {
          val server = creds.cloudID
          val user = creds.username
          val who = s"$user@$server"
          log info s"Authentication succeeded to: $who from: $remoteAddress"
          val intendedUrl = request.session.get(forms.intendedUri) getOrElse defaultLoginSuccessPage.url
          Redirect(intendedUrl).withSession(Security.username -> server.id)
        }).recoverAll(t => BadRequest(loginPage(cloudForm.withGlobalError("Invalid credentials."), flash)))
      }
    )
  }

  def loginPage(form: Form[CloudCreds], flash: Flash) =
    tags.login(form.globalError.map(_.message), flash.get(forms.feedback), this, None)

  def defaultLoginSuccessPage: Call = routes.Phones.rootFolder()

  def fut[T](body: => T) = Future successful body
}

object Web {
  private val log = Logger(getClass)
}
