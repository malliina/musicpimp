package org.musicpimp.js

import com.malliina.html.{Bootstrap, HtmlTags}
import com.malliina.musicpimp.js.CloudStrings
import com.malliina.musicpimp.models.*
import io.circe.Json
import org.musicpimp.js.Cloud.{CloudIdentifier, ConnectId, DisconnectId}
import org.scalajs.dom.{Event, HTMLInputElement, KeyboardEvent}
import scalatags.JsDom.all.*

case class UserFeedback(message: String, isError: Boolean)

object Cloud extends Bootstrap(HtmlTags):
  val ConnectId = "button-connect"
  val DisconnectId = "button-disconnect"
  val CloudIdentifier = "cloud-id"
  val inputId = "id"

  def connectingContent: Frag = leadPara("Connecting...")

  def disconnectingContent: Frag = leadPara("Disconnecting...")

  def connectedContent(id: CloudID): Frag =
    val msg =
      s"Connected. You can now access this server using your credentials and this cloud ID: $id"
    SeqFrag(
      Seq(
        halfRow(connectedForm()),
        halfRow(alertSuccess(msg))
      )
    )

  def disconnectedContent(reason: String): Frag =
    SeqFrag(
      Seq(
        halfRow(disconnectedForm()),
        halfRow(alertDanger(reason))
      )
    )

  private def disconnectedForm() =
    div(
      formGroup(
        label(`for` := inputId)("Desired cloud ID (optional)"),
        input(
          `type` := "text",
          id := CloudIdentifier,
          name := inputId,
          placeholder := "Your desired ID or leave empty",
          `class` := FormControl
        )
      ),
      cloudButton("Connect", ConnectId)
    )

  private def connectedForm() =
    div(
      cloudButton("Disconnect", DisconnectId)
    )

  private def cloudButton(title: String, buttonId: String) =
    blockSubmitButton(id := buttonId)(title)

class Cloud extends SocketJS("/ws/cloud?f=json") with CloudStrings:
  val formDiv = org.scalajs.dom.document.getElementById(CloudForm)
  override def onConnected(e: Event): Unit =
    send(Subscribe)
    super.onConnected(e)

  override def handlePayload(payload: Json): Unit =
    handleValidated[CloudEvent](payload): event =>
      val frag = event match
        case Connecting           => Cloud.connectingContent
        case Connected(id)        => Cloud.connectedContent(id)
        case Disconnected(reason) => Cloud.disconnectedContent(reason)
        case Disconnecting        => Cloud.disconnectingContent
      formDiv.innerHTML = ""
      formDiv.appendChild(frag.render)
      installHandlers()

  def installHandlers(): Unit =
    findElem(ConnectId).foreach(_.onClick(_ => connect()))
    findElem(DisconnectId).foreach(_.onClick(_ => send(Disconnect: CloudCommand)))
    findElem(CloudIdentifier).foreach(
      _.addEventListener[KeyboardEvent](
        "keypress",
        e =>
          val isEnter = e.keyCode == 10 || e.keyCode == 13
          if isEnter then connect()
      )
    )

  def connect(): Unit =
    send(Connect(CloudID(elemAs[HTMLInputElement](CloudIdentifier).value)): CloudCommand)
