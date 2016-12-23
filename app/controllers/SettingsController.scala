package controllers

import java.net.URLDecoder
import java.nio.file.{Files, Paths}

import akka.stream.Materializer
import com.malliina.musicpimp.db.Indexer
import com.malliina.musicpimp.library.{Library, Settings}
import com.malliina.musicpimp.tags.PimpTags
import com.malliina.play.Authenticator
import com.malliina.play.models.Username
import com.malliina.util.EnvUtils
import controllers.SettingsController.log
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages

import scala.util.Try

class SettingsController(tags: PimpTags,
                         messages: Messages,
                         indexer: Indexer,
                         auth: Authenticator,
                         mat: Materializer)
  extends HtmlController(auth, mat) {

  protected val newFolderForm = Form(
    "path" -> nonEmptyText.verifying("Not a directory", validateDirectory _)
  )
  private val folderPlaceHolder = if (EnvUtils.operatingSystem == EnvUtils.Windows) "C:\\music\\" else "/opt/music/"

  def manage = settings

  def settings = navigate(req => foldersPage(newFolderForm, req.user))

  def newFolder = pimpAction { request =>
    newFolderForm.bindFromRequest()(request).fold(
      formWithErrors => {
        log warn s"Errors: ${formWithErrors.errors}"
        BadRequest(foldersPage(formWithErrors, request.user))
      },
      path => {
        Settings.add(Paths get path)
        log info s"Added folder to music library: $path"
        onFoldersChanged()
      }
    )
  }

  def deleteFolder(folder: String) = pimpAction {
    val decoded = URLDecoder.decode(folder, "UTF-8")
    log info s"Attempting to remove folder: $decoded"
    val path = Paths get decoded
    Settings delete path
    log info s"Removed folder from music library: $decoded"
    onFoldersChanged()
  }

  def validateDirectory(dir: String) = Try(Files.isDirectory(Paths get dir)) getOrElse false

  private def foldersPage(form: Form[String], username: Username) =
    tags.musicFolders(Settings.readFolders, form, folderPlaceHolder, username, messages)

  private def onFoldersChanged() = {
    Library.reloadFolders()
    log info s"Music folders changed, reindexing..."
    indexer.indexAndSave()
    Redirect(routes.SettingsController.settings())
  }
}

object SettingsController {
  private val log = Logger(getClass)
}
