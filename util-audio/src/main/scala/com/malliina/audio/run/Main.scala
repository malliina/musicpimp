package com.malliina.audio.run

import java.nio.file.{Files, Path, Paths}

import org.apache.pekko.actor.ActorSystem
import com.malliina.audio.javasound.{FileJavaSoundPlayer, JavaSoundPlayer}
import com.malliina.storage.{StorageInt, StorageSize}

object Main:
  type ErrorMessage = String
  val noTrack = "Please specify a path to an MP3 as a command line parameter."

  case class Conf(path: Path, size: StorageSize)

  def main(args: Array[String]): Unit =
    val maybePath = args.headOption
      .fold[Either[ErrorMessage, Path]](Left(noTrack))(validate)
    val maybeStorage =
      args.tail.headOption.map(parseSize).getOrElse(Right(JavaSoundPlayer.DefaultRwBufferSize))
    val maybeConf = for
      path <- maybePath
      size <- maybeStorage
    yield Conf(path, size)
    maybeConf.fold(println, play)

  def play(conf: Conf): Unit =
    implicit val as = ActorSystem("run")
    val size = conf.size
    println(s"Playing with buffer size: $size.")
    val player = new FileJavaSoundPlayer(conf.path, conf.size)
    player.play()

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
