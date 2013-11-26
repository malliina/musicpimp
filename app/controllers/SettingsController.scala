package controllers

import play.api.data.Forms._
import play.api.data.Form
import views._
import java.nio.file.{Paths, Files}
import com.mle.musicpimp.library.{Library, Settings}
import com.mle.util.Log
import play.api.libs.json.Json._
import net.glxn.qrgen.QRCode
import play.api.libs.iteratee.Enumerator


/**
 * @author Michael
 */
trait SettingsController extends Secured with HtmlController with PimpAccountController with Log {
  protected val newFolderForm = Form(
    "path" -> nonEmptyText.verifying("Not a directory.", validateDirectory _)
  )

  def settings = navigate(html.musicFolders(Settings.readFolders, newFolderForm))



  def newFolder = PimpAction(implicit req => {
    newFolderForm.bindFromRequest.fold(
      formWithErrors => {
        log info s"Errors: ${formWithErrors.errors}"
        BadRequest(html.musicFolders(Settings.readFolders, formWithErrors))
      },
      path => {
        Settings.add(Paths get path)
        log info s"Added folder to music library: $path"
        onFoldersChanged
      }
    )
  })

  def deleteFolder(folder: String) = PimpAction {
    val path = Paths get folder
    Settings delete path
    log info s"Removed folder from music library: $folder"
    onFoldersChanged
  }

  def validateDirectory(dir: String) = Files.isDirectory(Paths get dir)

  private def onFoldersChanged = {
    Library.rootFolders = Settings.read
    Redirect(routes.Website.settings())
  }
}
