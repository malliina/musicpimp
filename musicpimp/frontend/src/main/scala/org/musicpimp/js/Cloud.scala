package org.musicpimp.js

import com.malliina.musicpimp.js.{CloudStrings, FrontStrings}
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
  val Connected = "connected"
  val Connecting = "connecting"
  val Disconnected = "disconnected"
  val Disconnecting = "disconnecting"
  val ConnectId = "button-connect"
  val DisconnectId = "button-disconnect"
  val CloudId = "cloud-id"
  val ConnectCmd = "connect"
  val DisconnectCmd = "disconnect"
  val inputId = "id"
  val Reason = "reason"
  val IdKey = "id"

  def connectingContent: Frag = leadPara("Connecting...")

  def disconnectingContent: Frag = leadPara("Disconnecting...")

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
        input(
          `type` := Text,
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

class Cloud extends SocketJS("/ws/cloud?f=json") with CloudStrings {
  val formDiv = elem(CloudForm)

  override def onConnected(e: Event) = {
    send(Command.subscribe)
    super.onConnected(e)
  }

  override def handlePayload(payload: Value) = {
    onSocketEvent(payload)
  }

  def onSocketEvent(payload: Value) = {
    val fragment: Either[Invalid, Frag] =
      readField[String](payload, FrontStrings.EventKey).flatMap {
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
        case Disconnecting =>
          Right(Cloud.disconnectingContent)
        case other =>
          Left(Invalid.Data(payload, s"Unknown event '$other'."))
      }
    fragment.fold(onJsonFailure, frag => formDiv.html(frag.render))
    installHandlers()
  }

  def installHandlers() = {
    elem(ConnectId).click((_: JQueryEventObject) => {
      connect()
    })
    elem(DisconnectId).click((_: JQueryEventObject) => {
      send(Command(DisconnectCmd))
    })
    elem(CloudId).keypress((e: JQueryEventObject) => {
      val isEnter = e.which == 10 || e.which == 13
      if (isEnter) {
        connect()
      }
    })
  }

  def connect() = {
    send(IdCommand(ConnectCmd, elem(CloudId).value().toString))
  }
}
