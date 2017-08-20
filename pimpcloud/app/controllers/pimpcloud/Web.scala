package controllers.pimpcloud

import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.models.{CloudID, CloudIDs}
import com.malliina.pimpcloud.auth.{CloudAuthentication, CloudCredentials}
import com.malliina.play.auth.Auth
import com.malliina.play.controllers.{AccountForms, Caching}
import com.malliina.play.http.Proxies
import com.malliina.play.models.{Password, Username}
import controllers.pimpcloud.Web.{cloudForm, forms, log}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

import scala.concurrent.Future

case class CloudCreds(cloudID: CloudID, username: Username, pass: Password)

object Web {
  private val log = Logger(getClass)

  val forms = new AccountForms
  val serverFormKey = "server"

  val cloudForm = Form[CloudCreds](mapping(
    serverFormKey -> CloudIDs.mapping,
    forms.userFormKey -> Username.mapping,
    forms.passFormKey -> Password.mapping
  )(CloudCreds.apply)(CloudCreds.unapply))
}

class Web(comps: ControllerComponents,
          tags: CloudTags,
          authActions: CloudAuthentication)
  extends AbstractController(comps) {
  implicit val ec = defaultExecutionContext

  def ping = Action {
    Caching.NoCache(Ok)
  }

  def login = Action { req =>
    Ok(loginPage(cloudForm, req.flash))
  }

  def formAuthenticate = Action.async { request =>
    val flash = request.flash
    val remoteAddress = Proxies.realAddress(request)
    cloudForm.bindFromRequest()(request).fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(forms.userFormKey, "")
        log warn s"Authentication failed for user: $user from '$remoteAddress'."
        fut(BadRequest(loginPage(formWithErrors, flash)))
      },
      cloudCreds => {
        val creds = CloudCredentials(cloudCreds.cloudID, cloudCreds.username, cloudCreds.pass, request)
        authActions.authWebClient(creds).map(_ => {
          val server = creds.cloudID
          val user = creds.username
          val who = s"$user@$server"
          log info s"Authentication succeeded to '$who' from '$remoteAddress'."
          val intendedUrl = request.session.get(forms.intendedUri) getOrElse defaultLoginSuccessPage.url
          Redirect(intendedUrl).withSession(Auth.DefaultSessionKey -> server.id)
        }).recoverAll(_ => BadRequest(loginPage(cloudForm.withGlobalError("Invalid credentials."), flash)))
      }
    )
  }

  def loginPage(form: Form[CloudCreds], flash: Flash) =
    tags.login(form.globalError.map(_.message), flash.get(forms.feedback), None)

  def defaultLoginSuccessPage: Call = routes.Phones.rootFolder()

  def fut[T](body: => T) = Future successful body
}
