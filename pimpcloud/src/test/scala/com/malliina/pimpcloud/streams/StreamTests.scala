package com.malliina.pimpcloud.streams

import cats.effect.IO
import fs2.concurrent.Topic

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class StreamTests extends munit.CatsEffectSuite:
  test("hej".ignore):
    val task = Topic[IO, Option[FiniteDuration]].flatMap: t =>
      fs2.Stream
        .awakeEvery[IO](1.second)
        .evalMap(d => t.publish1(Option(d)).map(_.map(_ => d).toOption))
        .concurrently(fs2.Stream.eval(IO.defer(t.close.void).delayBy(2.seconds)))
        .takeWhile(_.isDefined)
        .compile
        .toList
    task.map: durs =>
      durs foreach println
      assertEquals(durs.size, 7)
