package com.malliina.pimpcloud.ws

import org.apache.pekko.actor.{Actor, ActorRef, Terminated}
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.ws.ServerMediator.*
import com.malliina.pimpcloud.{PimpServer, PimpServers, PimpStream, PimpStreams}
import com.malliina.play.http.Proxies
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps

class ServerMediator extends Actor:
  var servers: Set[PimpServerSocket] = Set.empty
  var listeners: Set[ActorRef] = Set.empty

  def serversJson: PimpServers =
    val ss = servers map { server =>
      PimpServer(server.id, Proxies.realAddress(server.headers))
    }
    PimpServers(ss.toSeq)

  def ongoing = servers.flatMap(_.fileTransfers.snapshot)

  def ongoingJson(streams: Set[PimpStream]): PimpStreams =
    PimpStreams(streams.toSeq)

  def receive: Receive = {
    case Listen(listener) =>
      context.watch(listener)
      listeners += listener
      listener ! ongoingJson(ongoing).asJson
      listener ! serversJson.asJson
    case ServerJoined(server, out) =>
      context.watch(out)
      servers += server
      sendUpdate(serversJson)
    case Exists(id) =>
      val exists = servers.exists(_.id == id)
      sender() ! exists
    case GetServers =>
      sender() ! servers
    case StreamsUpdated =>
      sendUpdate(ongoingJson(ongoing))
    case Terminated(out) =>
      servers.find(_.jsonOut == out) foreach { server =>
        servers -= server
        sendUpdate(serversJson)
      // TODO kill all phones connected to this server
      }
      listeners.find(_ == out) foreach { listener =>
        listeners -= listener
      }
  }

  def sendUpdate[C: Encoder](message: C): Unit =
    listeners foreach { listener =>
      listener ! message.asJson
    }

object ServerMediator:
  case class ServerJoined(server: PimpServerSocket, out: ActorRef)
  case class ServerEvent(message: Json, from: CloudID)
  case class Exists(id: CloudID)
  case object GetServers
  case object StreamsUpdated
  case class Listen(listener: ActorRef)
