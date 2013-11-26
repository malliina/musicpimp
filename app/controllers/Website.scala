package controllers

import views.html
import com.mle.util.Log
import javax.sound.sampled.AudioSystem
import com.mle.musicpimp.library.Settings

/**
 *
 * @author mle
 */
object Website
  extends Secured
  with HtmlController
  with SettingsController
  with LibraryController
  with PimpAccountController
  with Logs
  with Log {

  def player = navigate(implicit req => {
    val hasAudioDevice = AudioSystem.getMixerInfo.size > 0
    val feedback =
      if (!hasAudioDevice) {
        Some("Unable to find audio hardware. Playback on this machine is likely to fail.")
      } else {
        None
      }
    html.player(feedback)
  })

  def popupPlayer = navigate(implicit req => html.popupPlayer())

  def manage = navigate(html.musicFolders(Settings.readFolders, newFolderForm))

  def about = navigate(html.aboutBase())

  def parameters = navigate(html.parameters(Rest.uploadDir.toAbsolutePath.toString))
}