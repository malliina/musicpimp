package com.malliina.beam

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import com.malliina.beam.PlayerClient.log
import com.malliina.values.Username
import play.api.Logger

object PlayerClient:
  private val log = Logger(getClass)

class PlayerClient(user: Username, out: ActorRef, mat: Materializer) extends BeamClient(user, out):

  @volatile
  var streamer = StreamManager.empty(mat)

  log debug s"Created stream for user: $user"

  def stream: Source[ByteString, ?] = streamer.stream

  /** Ends the current stream and replaces it with a new one, then sends a reset message to the
    * client so that the client will receive the newly created stream instead.
    *
    * This is used when the user starts playing a new track, discarding any currently playing
    * stream.
    */
  def resetStream(): Unit =
    log info s"Resetting streaming for '$user'..."
    streamer.close()
    streamer = StreamManager.empty(mat)
    // instructs the client to GET /stream
    out ! BeamMessages.reset
