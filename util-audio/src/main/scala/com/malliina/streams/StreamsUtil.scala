package com.malliina.streams

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import org.apache.pekko.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}

trait EventSink[U]:
  def send(event: U): Unit

class StreamSink[U](val sink: Sink[U, NotUsed])(implicit mat: Materializer) extends EventSink[U]:
  override def send(event: U): Unit = Source.single(event).to(sink).run()

/** Events of type `U` sent to `sink` will be emitted by `source`.
  *
  * Use `killSwitch.shutdown()` to shut down.
  *
  * @param sink
  *   sink of events; use `sink.send(u: U)` to send
  * @param source
  *   emitter of events
  * @param killSwitch
  *   kill switch
  * @tparam U
  *   type of events
  */
case class ConnectedStream[U](
  sink: StreamSink[U],
  source: Source[U, NotUsed],
  killSwitch: UniqueKillSwitch
):
  def send(event: U): Unit = sink.send(event)
  def shutdown(): Unit = killSwitch.shutdown()

object StreamsUtil:
  def connectedStream[U]()(implicit mat: Materializer): ConnectedStream[U] =
    val killSwitchGraph = KillSwitches.single[U]
    val ((sink, killSwitch), source) = MergeHub
      .source[U](perProducerBufferSize = 16)
      .viaMat(killSwitchGraph)(Keep.both)
      .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
      .run()
    val _ = source.runWith(Sink.ignore)
    ConnectedStream(new StreamSink[U](sink), source, killSwitch)
