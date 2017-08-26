package org.musicpimp.js

import com.malliina.musicpimp.js.CloudStrings
import com.malliina.musicpimp.models.CloudEvent.{Connected, Connecting, Disconnected, Disconnecting}
import com.malliina.musicpimp.models._
import com.malliina.tags.Bootstrap._
import com.malliina.tags.Tags._
import org.musicpimp.js.Cloud._
import org.scalajs.dom.raw.Event
import org.scalajs.jquery.JQueryEventObject
import play.api.libs.json.JsValue

import scalatags.Text.Frag
import scalatags.Text.all._

case class UserFeedback(message: String, isError: Boolean)

object Cloud {
  val ConnectId = "button-connect"
  val DisconnectId = "button-disconnect"
  val CloudIdentifier = "cloud-id"
  val inputId = "id"

  def connectingContent: Frag = leadPara("Connecting...")

  def disconnectingContent: Frag = leadPara("Disconnecting...")

  def connectedContent(id: CloudID): Frag = {
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
          `type` := "text",
          id := CloudIdentifier,
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

  override def onConnected(e: Event): Unit = {
    send(Subscribe)
    super.onConnected(e)
  }

  override def handlePayload(payload: JsValue): Unit = {
    handleValidated[CloudEvent](payload) { event =>
      val frag = event match {
        case Connecting => Cloud.connectingContent
        case Connected(id) => Cloud.connectedContent(id)
        case Disconnected(reason) => Cloud.disconnectedContent(reason)
        case Disconnecting => Cloud.disconnectingContent
      }
      formDiv.html(frag.render)
      installHandlers()
    }
  }

  def installHandlers() = {
    elem(ConnectId).click { (_: JQueryEventObject) =>
      connect()
    }
    elem(DisconnectId).click { (_: JQueryEventObject) =>
      send(Disconnect)
    }
    elem(CloudIdentifier).keypress { (e: JQueryEventObject) =>
      val isEnter = e.which == 10 || e.which == 13
      if (isEnter) {
        connect()
      }
    }
  }

  def connect(): Unit =
    send(Connect(CloudID(elem(CloudIdentifier).value().toString)))
}
