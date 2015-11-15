package controllers

import java.net.URLDecoder
import java.nio.file.{Files, Paths}

import com.mle.musicpimp.db.Indexer
import com.mle.musicpimp.library.{Library, Settings}
import com.mle.play.Authenticator
import com.mle.util.{EnvUtils, Log}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import views.html

import scala.util.Try

/**
 * @author Michael
 */
class SettingsController(messages: Messages, indexer: Indexer, auth: Authenticator)
  extends HtmlController(auth)
  with Log {

  protected val newFolderForm = Form(
    "path" -> nonEmptyText.verifying("Not a directory", validateDirectory _)
  )
  private val folderPlaceHolder = if (EnvUtils.operatingSystem == EnvUtils.Windows) "C:\\music\\" else "/opt/music/"

  def manage = settings

  def settings = navigate(foldersPage(newFolderForm))

  def newFolder = PimpAction(implicit req => {
    newFolderForm.bindFromRequest.fold(
      formWithErrors => {
        log warn s"Errors: ${formWithErrors.errors}"
        BadRequest(foldersPage(formWithErrors))
      },
      path => {
        Settings.add(Paths get path)
        log info s"Added folder to music library: $path"
        onFoldersChanged()
      }
    )
  })

  def deleteFolder(folder: String) = PimpAction {
    val decoded = URLDecoder.decode(folder, "UTF-8")
    log info s"Attempting to remove folder: $decoded"
    val path = Paths get decoded
    Settings delete path
    log info s"Removed folder from music library: $decoded"
    onFoldersChanged()
  }

  def validateDirectory(dir: String) = Try(Files.isDirectory(Paths get dir)) getOrElse false

  private def foldersPage(form: Form[String]) = {
    html.musicFolders(Settings.readFolders, form, folderPlaceHolder)(messages)
  }

  private def onFoldersChanged() = {
    Library.reloadFolders()
    log info s"Music folders changed, reindexing..."
    indexer.indexAndSave()
    Redirect(routes.SettingsController.settings())
  }
}
