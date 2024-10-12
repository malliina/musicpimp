package tests

import com.malliina.audio.javasound.{FileJavaSoundPlayer, JavaSoundPlayer}
import munit.FunSuite
import org.apache.commons.io.FileUtils
import org.apache.pekko.actor.ActorSystem

import java.nio.file.{Files, Path, Paths}
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}

class TestBase extends FunSuite {
  implicit val as: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = as.dispatcher

//  override protected def afterAll(): Unit = {
//    await(as.terminate())
//    super.afterAll()
//  }

  def await[T](f: Future[T], duration: FiniteDuration = 10.seconds) = Await.result(f, duration)

  val fileName = "mpthreetest.mp3"
  val tempFile = Paths.get(sys.props("java.io.tmpdir")).resolve(fileName)

  def ensureTestMp3Exists(): Path = {
    if (!Files.exists(tempFile)) {
      val resourceURL = Option(getClass.getClassLoader.getResource(fileName))
      val url = resourceURL.getOrElse(throw new Exception(s"Resource not found: " + fileName))
      FileUtils.copyURLToFile(url, tempFile.toFile)
      if (!Files.exists(tempFile)) {
        throw new Exception(s"Unable to access $tempFile")
      }
    }
    tempFile
  }

  def withTestTrack[T](f: JavaSoundPlayer => T): T = {
    val file = ensureTestMp3Exists()
    val player = new FileJavaSoundPlayer(file)
    try {
      f(player)
    } finally {
      player.close()
    }
  }

  def assertPosition(pos: Duration, min: Long, max: Long) = {
    val seconds = pos.toSeconds
    assert(seconds >= min && seconds <= max, s"$seconds must be within [$min, $max]")
  }

  def sleep(duration: Duration): Unit = Thread.sleep(duration.toMillis)
}
