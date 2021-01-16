package controllers.musicpimp

import java.net.URLDecoder
import java.nio.file.{Files, Paths}

import com.malliina.musicpimp.db.Indexer
import com.malliina.musicpimp.html.{LibraryContent, PimpHtml}
import com.malliina.musicpimp.library.{FileLibrary, Settings}
import com.malliina.musicpimp.messaging._
import com.malliina.musicpimp.messaging.adm.AmazonDevices
import com.malliina.musicpimp.messaging.apns.APNSDevices
import com.malliina.musicpimp.messaging.gcm.GoogleDevices
import com.malliina.musicpimp.messaging.mpns.PushUrls
import com.malliina.util.EnvUtils
import controllers.musicpimp.SettingsController.log
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages
import play.api.libs.json.Json

import scala.util.Try

object SettingsController {
  private val log = Logger(getClass)

  val Path = "path"
}

class SettingsController(
  tags: PimpHtml,
  messages: Messages,
  library: FileLibrary,
  indexer: Indexer,
  auth: AuthDeps
) extends HtmlController(auth) {
  val dirConstraint = Constraint((dir: String) =>
    if (validateDirectory(dir)) Valid
    else Invalid(Seq(ValidationError(s"Invalid directory '$dir'.")))
  )

  protected val newFolderForm = Form(
    SettingsController.Path -> nonEmptyText.verifying(dirConstraint)
  )
  private val folderPlaceHolder = EnvUtils.operatingSystem match {
    case EnvUtils.Windows => "C:\\music\\"
    case EnvUtils.Mac     => "/Users/me/music"
    case _                => "/opt/music"
  }

  def manage = settings

  def tokens = pimpAction {
    val ts = APNSDevices.get().map(d => TokenInfo(d.id, Apns)) ++
      PushUrls.get().map(p => TokenInfo(p.url, Mpns)) ++
      GoogleDevices.get().map(g => TokenInfo(g.id, Gcm)) ++
      AmazonDevices.get().map(a => TokenInfo(a.id, Adm))
    Ok(Json.toJson(Tokens(ts)))
  }

  def settings = navigate(req => foldersPage(newFolderForm, req))

  def newFolder = pimpAction { request =>
    newFolderForm
      .bindFromRequest()(request, formBinding)
      .fold(
        formWithErrors => {
          log warn s"Errors: ${formWithErrors.errors}"
          BadRequest(foldersPage(formWithErrors, request))
        },
        path => {
          Settings.add(Paths get path)
          onFoldersChanged(s"Added folder '$path'.")
        }
      )
  }

  def deleteFolder(folder: String) = pimpAction {
    val decoded = URLDecoder.decode(folder, "UTF-8")
    log info s"Attempting to remove folder '$decoded'..."
    val path = Paths get decoded
    Settings delete path
    onFoldersChanged(s"Removed folder '$decoded'.")
  }

  def validateDirectory(dir: String) = Try(Files.isDirectory(Paths get dir)) getOrElse false

  private def foldersPage(form: Form[String], req: PimpUserRequest) = {
    val errorMessage = form.errors.headOption.map { error =>
      UserFeedback.error(Messages(error.message)(messages))
    }
    val flashMessage = UserFeedback.flashed(req.flash)
    val feedback = errorMessage orElse flashMessage
    tags.musicFolders(LibraryContent(Settings.readFolders, folderPlaceHolder, req.user, feedback))
  }

  private def onFoldersChanged(successMessage: String) = {
    log info s"$successMessage"
    library.reloadFolders()
    indexer.indexAndSave()
    Redirect(routes.SettingsController.settings())
      .flashing(UserFeedback.Feedback -> successMessage)
  }
}
