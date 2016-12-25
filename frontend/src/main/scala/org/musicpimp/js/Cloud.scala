package org.musicpimp.js

import com.malliina.tags.Bootstrap._
import com.malliina.tags.Tags._
import org.scalajs.dom.raw.Event
import org.scalajs.jquery.JQueryEventObject
import upickle.Invalid
import upickle.Js.Value
import Cloud._
import scalatags.Text.all._
import scalatags.Text.{Frag, TypedTag}

case class UserFeedback(message: String, isError: Boolean)

object Cloud {
  val Connecting = "connecting"
  val Connected = "connected"
  val Disconnected = "disconnected"
  val ConnectId = "button-connect"
  val DisconnectId = "button-disconnect"
  val CloudId = "cloud-id"
  val ConnectCmd = "connect"
  val DisconnectCmd = "disconnect"
  val ToggleButton = "toggleButton"
  val inputId = "id"

  def connectingContent: Frag = leadPara("Connecting...")

  def connectedContent(id: String): Frag = {
    val msg = s"Connected. You can now access this server using your credentials and this cloud ID: $id"
    SeqFrag(Seq(
      halfRow(connectedForm()),
      halfRow(alertSuccess(msg))
    ))
  }

  def disconnectedContent(reason: String): Frag = {
    SeqFrag(Seq(
      halfRow(disconnectedForm()),
      halfRow(alertDanger(reason))
    ))
  }

  private def disconnectedForm() = cloudForm(
    formGroup(
      labelFor(inputId)("Desired cloud ID (optional)"),
      input(`type` := Text,
        id := CloudId,
        name := inputId,
        placeholder := "Your desired ID or leave empty",
        `class` := FormControl)
    ),
    cloudButton("Connect", ConnectId)
  )

  private def connectedForm() = cloudForm(
    cloudButton("Disconnect", DisconnectId)
  )

  private def cloudButton(title: String, buttonId: String) =
    blockSubmitButton(id := buttonId)(title)

  private def cloudForm = postableForm("/cloud", name := "toggleForm")

  private def postableForm(onAction: String, more: Modifier*) =
    div

  private def feedbackDiv(feedback: UserFeedback): TypedTag[String] = {
    val message = feedback.message
    if (feedback.isError) alertDanger(message)
    else alertSuccess(message)
  }
}

class Cloud extends SocketJS("/ws/cloud?f=json") {
  val formDiv = elem("cloud-form")

  override def onConnected(e: Event) = {
    send(Command.subscribe)
    super.onConnected(e)
  }

  override def handlePayload(payload: Value) = {
    onSocketEvent(payload)
  }

  def onSocketEvent(payload: Value) = {
    val fragment: Either[Invalid, Frag] =
      readField[String](payload, "event").right.flatMap {
        case Connecting =>
          Right(Cloud.connectingContent)
        case Connected =>
          readField[String](payload, "id").right.map { id =>
            Cloud.connectedContent(id)
          }
        case Disconnected =>
          readField[String](payload, "reason").right.map { reason =>
            Cloud.disconnectedContent(reason)
          }
        case other =>
          Left(Invalid.Data(payload, s"Unknown event: '$other'."))
      }
    fragment.fold(onInvalidData, frag => formDiv.html(frag.render))
    installHandlers()
  }

  def installHandlers() = {
    elem(ConnectId).click((e: JQueryEventObject) => {
      send(IdCommand(ConnectCmd, elem(CloudId).value().toString))
    })
    elem(DisconnectId).click((e: JQueryEventObject) => {
      send(Command(DisconnectCmd))
    })
  }
}
