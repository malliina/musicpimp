package com.malliina.musicpimp.http4s

import com.malliina.html.UserFeedback
import com.malliina.http.FullUrl
import com.malliina.musicpimp.models.{FailReason, TrackID}
import com.malliina.musicpimp.scheduler.WeekDay
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import com.malliina.util.{AppLogger, EnvUtils}
import com.malliina.values.{Password, Username}
import io.circe.Codec

import java.net.ConnectException
import java.nio.file.{Files, Paths}
import javax.sound.sampled.LineUnavailableException
import scala.util.Try

object Accounts:
  val UsersFeedback = "usersFeedback"

  val invalidCredentialsMessage = "Invalid credentials."
  val passwordChangedMessage = "Password successfully changed."
  val logoutMessage = "You have now logged out."
  val incorrectPasswordMessage = "Incorrect password."
  val repeatPassFailureMessage = "The password was incorrectly repeated."
  val cannotDeleteYourself = "You cannot delete yourself."

  def defaultCredentialsMessage(user: Username, pass: Password) =
    s"Welcome! The default credentials of $user / ${pass.pass} have not been changed. " +
      s"Consider changing the password under the Manage tab once you have logged in."

object AlarmEditor:
  def parseDaysEnabledAndJob(
    days: Seq[String],
    enabledOpt: Option[String]
  ): (Seq[WeekDay], Boolean) =
    val weekDays = days.flatMap(WeekDay.withShortName)
    val enabled = enabledOpt.contains(SchedulerStrings.On)
    (weekDays, enabled)

object Cloud:
  private val log = AppLogger(getClass)
  val idFormKey = "id"

  case class ToggleCloudId(id: Option[String]) derives Codec.AsObject

  def errorMessage(t: Throwable, uri: FullUrl): String =
    t match
      case ce: ConnectException =>
        log.error(s"Unable to connect to $uri.", ce)
        "Unable to connect to the cloud. Please try again later."
      case t: Throwable =>
        val msg = "Unable to connect to the cloud."
        log.error(msg, t)
        msg

object CloudWS:
  val ConnectCmd = "connect"
  val DisconnectCmd = "disconnect"
  val Id = "id"

object LibraryController:
  def noTrackJson(id: TrackID) = FailReason(s"Track not found: $id")

object Playlists:
  val Id = "id"
  val Name = "name"
  val Tracks = "tracks"

object Search:
  val DefaultLimit = 1000

object SettingsController:
  val Path = "path"

  val folderPlaceHolder = EnvUtils.operatingSystem match
    case EnvUtils.Windows => "C:\\music\\"
    case EnvUtils.Mac     => "/Users/me/music"
    case _                => "/opt/music"

  def validateDirectory(dir: String) = Try(Files.isDirectory(Paths.get(dir))) getOrElse false

object Website:
  def errorMsg(t: Throwable): String = t match
    case _: LineUnavailableException =>
      "Playback could not be started. To troubleshoot this issue, you may wish to verify that audio " +
        "playback is possible on the server and that the audio drivers are working. Check the sound " +
        "properties of your Java Virtual Machine. If you use OpenJDK, you may want to try Oracle's JVM " +
        "instead and vice versa. The playback exception is a LineUnavailableException."
    case t: Throwable =>
      val msg = Option(t.getMessage).getOrElse("")
      s"Playback could not be started. $msg"

object UserFeedbackUtil:
  val Feedback = "feedback"
  val Success = "success"
  val Yes = "yes"
  val No = "no"

  def success(message: String) = UserFeedback(message, isError = false)

  def error(message: String) = UserFeedback(message, isError = true)
