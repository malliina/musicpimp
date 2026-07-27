package tests

import cats.effect.IO
import cats.effect.std.Dispatcher
import com.malliina.audio.javasound.{BasicJavaSoundPlayer, JavaSoundPlayer}
import com.malliina.audio.meta.OneShotStream
import org.apache.commons.io.FileUtils

import java.nio.file.{Files, Path, Paths}
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}

class TestBase extends munit.CatsEffectSuite:
  def filePlayer(file: Path) =
    Dispatcher
      .parallel[IO]
      .evalMap: d =>
        BasicJavaSoundPlayer.fromFile(file, d)

  def soundPlayer(stream: OneShotStream) =
    Dispatcher
      .parallel[IO]
      .evalMap: d =>
        JavaSoundPlayer.default(stream, d)

  def await[T](f: Future[T], duration: FiniteDuration = 10.seconds) = Await.result(f, duration)

  val unit = IO.unit

  val fileName = "mpthreetest.mp3"
  val tempFile = Paths.get(sys.props("java.io.tmpdir")).resolve(fileName)

  def ensureTestMp3Exists(): Path =
    if !Files.exists(tempFile) then
      val resourceURL = Option(getClass.getClassLoader.getResource(fileName))
      val url = resourceURL.getOrElse(throw new Exception(s"Resource not found: " + fileName))
      FileUtils.copyURLToFile(url, tempFile.toFile)
      if !Files.exists(tempFile) then throw new Exception(s"Unable to access $tempFile")
    tempFile

  def withTestTrack[T](f: JavaSoundPlayer[IO] => IO[T]): IO[T] =
    val file = ensureTestMp3Exists()
    filePlayer(file).use: player =>
      f(player).attemptTap(_ => IO.delay(player.close()))

  def assertPosition(pos: Duration, min: Long, max: Long) =
    val seconds = pos.toSeconds
    assert(seconds >= min && seconds <= max, s"$seconds must be within [$min, $max]")

  def sleep(duration: Duration): Unit = Thread.sleep(duration.toMillis)
