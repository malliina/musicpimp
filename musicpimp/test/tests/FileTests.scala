package tests

import java.nio.file.Path

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.malliina.musicpimp.library.Library
import com.malliina.util.Utils
import org.scalatest.FunSuite

import scala.concurrent.duration.{DurationDouble, DurationInt}

class FileTests extends FunSuite {
  val minPathsPerSecond = 400
  val testRootOpt: Option[Path] = None
  val as = ActorSystem("test")
  val mat = ActorMaterializer()(as)
  val library = Library(mat)

  test(s"lists at least $minPathsPerSecond files per second or completes within 3 seconds") {
    testRootOpt foreach { testRoot =>
      library.setFolders(Seq(testRoot))
      val (size, duration) = Utils.timed(library.trackFiles.size)
      val threshold = (1.0d * size / minPathsPerSecond).seconds
      assert(duration < 3.seconds || duration < threshold)
    }
  }

  test("folder list") {
    testRootOpt foreach { testRoot =>
      library.setFolders(Seq(testRoot))
      val (size, duration) = Utils.timed(library.folderStream.size)
      val threshold = (1.0d * size / minPathsPerSecond).seconds
      assert(duration < 3.seconds || duration < threshold)
    }
  }
}
