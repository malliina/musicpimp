package com.malliina.audio.run

import cats.effect.IO
import cats.effect.std.Dispatcher
import cats.effect.unsafe.implicits.global

import java.nio.file.{Files, Path, Paths}
import org.apache.pekko.actor.ActorSystem
import com.malliina.audio.javasound.{BasicJavaSoundPlayer, JavaSoundPlayer}
import com.malliina.storage.{StorageInt, StorageSize}

import javax.sound.sampled.AudioSystem

object Main:
  type ErrorMessage = String
  private val noTrack = "Please specify a path to an MP3 as a command line parameter."

  case class Conf(path: Path, size: StorageSize)

  def main(args: Array[String]): Unit =
    val v = Runtime.version().toString
    println(s"Java $v")
    val mixers = AudioSystem.getMixerInfo
    val message =
      if mixers.isEmpty then "Got no mixers, playback is likely to fail"
      else
        val describe =
          mixers.map(m => s"${m.getName} (${m.getDescription})").mkString("\n", "\n", "\n")
        s"Got ${mixers.size} mixers: $describe"
    println(message)
    def maybePath = args.headOption
      .fold[Either[ErrorMessage, Path]](Left(noTrack))(validate)
    def maybeStorage =
      args.tail.headOption.map(parseSize).getOrElse(Right(JavaSoundPlayer.DefaultRwBufferSize))
    val maybeConf = for
      path <- maybePath
      size <- maybeStorage
    yield Conf(path, size)
    val (d, finalizer) = Dispatcher.parallel[IO].allocated.unsafeRunSync()
    maybeConf.fold(println, c => play(c, d))

  def play(conf: Conf, d: Dispatcher[IO]): IO[Unit] =
    val size = conf.size
    println(s"Playing with buffer size: $size.")
    BasicJavaSoundPlayer.fromFile[IO](conf.path, d, conf.size).map(_.play())

  def parseSize(input: String): Either[ErrorMessage, StorageSize] =
    try Right(input.toInt.bytes)
    catch
      case _: NumberFormatException =>
        Left(s"Not a number: $input")

  def validate(input: String): Either[ErrorMessage, Path] =
    val path = Paths.get(input)
    val absolute = path.toAbsolutePath
    if !Files.exists(absolute) then Left(s"File does not exist: $absolute.")
    else if !Files.isReadable(absolute) then Left(s"File is not readable: $absolute.")
    else Right(absolute)
