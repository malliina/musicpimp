package tests

import com.malliina.musicpimp.library.Library
import com.malliina.util.Util
import org.apache.pekko.actor.ActorSystem

import java.nio.file.Path
import scala.concurrent.duration.{DurationDouble, DurationInt}

class FileTests extends munit.FunSuite:
  val minPathsPerSecond = 400
  val testRootOpt: Option[Path] = None
  implicit val as: ActorSystem = ActorSystem("test")
  val library = Library()

  test(s"lists at least $minPathsPerSecond files per second or completes within 3 seconds"):
    testRootOpt foreach { testRoot =>
      library.setFolders(Seq(testRoot))
      val (size, duration) = Util.timed(library.trackFiles.size)
      val threshold = (1.0d * size / minPathsPerSecond).seconds
      assert(duration < 3.seconds || duration < threshold)
    }

  test("folder list"):
    testRootOpt foreach { testRoot =>
      library.setFolders(Seq(testRoot))
      val (size, duration) = Util.timed(library.folderStream.size)
      val threshold = (1.0d * size / minPathsPerSecond).seconds
      assert(duration < 3.seconds || duration < threshold)
    }
