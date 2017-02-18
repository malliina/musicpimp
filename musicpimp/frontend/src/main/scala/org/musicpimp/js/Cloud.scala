package org.musicpimp.js

import com.malliina.tags.Bootstrap._
import com.malliina.tags.Tags._
import org.musicpimp.js.Cloud._
import org.scalajs.dom.raw.Event
import org.scalajs.jquery.JQueryEventObject
import upickle.Invalid
import upickle.Js.Value

import scalatags.Text.Frag
import scalatags.Text.all._

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
  val EventKey = "event"
  val Reason ="reason"
  val IdKey ="id"

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

  private def disconnectedForm() =
    div(
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

  private def connectedForm() =
    div(
      cloudButton("Disconnect", DisconnectId)
    )

  private def cloudButton(title: String, buttonId: String) =
    blockSubmitButton(id := buttonId)(title)
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
      readField[String](payload, EventKey).flatMap {
        case Connecting =>
          Right(Cloud.connectingContent)
        case Connected =>
          readField[String](payload, IdKey).map { id =>
            Cloud.connectedContent(id)
          }
        case Disconnected =>
          readField[String](payload, Reason).map { reason =>
            Cloud.disconnectedContent(reason)
          }
        case other =>
          Left(Invalid.Data(payload, s"Unknown event: '$other'."))
      }
    fragment.fold(onJsonFailure, frag => formDiv.html(frag.render))
    installHandlers()
  }

  def installHandlers() = {
    elem(ConnectId).click((_: JQueryEventObject) => {
      send(IdCommand(ConnectCmd, elem(CloudId).value().toString))
    })
    elem(DisconnectId).click((_: JQueryEventObject) => {
      send(Command(DisconnectCmd))
    })
  }
}
