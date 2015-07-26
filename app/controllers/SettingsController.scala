package controllers

import java.net.URLDecoder
import java.nio.file.{Files, Paths}

import com.mle.musicpimp.db.Indexer
import com.mle.musicpimp.library.{Library, Settings}
import com.mle.util.{EnvUtils, Log}
import play.api.{Play, Environment}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{Lang, I18nSupport, Messages, MessagesApi}

//import play.api.i18n.MessagesApi
import views._

import scala.util.Try

/**
 * @author Michael
 */
object SettingsController extends Secured with HtmlController with Log {
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
    implicit val messages = Messages.Implicits.applicationMessages(Lang.defaultLang, Play.current)
    html.musicFolders(Settings.readFolders, form, folderPlaceHolder)
  }

  private def onFoldersChanged() = {
    Library.rootFolders = Settings.read
    log info s"Music folders changed, reindexing..."
    Indexer.indexAndSave()
    Redirect(routes.SettingsController.settings())
  }
}
