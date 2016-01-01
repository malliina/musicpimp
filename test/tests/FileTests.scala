package tests

import java.nio.file.Path

import com.malliina.musicpimp.library.Library
import com.malliina.util.Utils
import org.scalatest.FunSuite

import scala.concurrent.duration.{DurationDouble, DurationInt}

/**
 * @author Michael
 */
class FileTests extends FunSuite {
  val minPathsPerSecond = 400
  val testRootOpt: Option[Path] = None

  test(s"lists at least $minPathsPerSecond files per second or completes within 3 seconds") {
    testRootOpt.foreach(testRoot => {
      Library.setFolders(Seq(testRoot))
      val (size, duration) = Utils.timed(Library.trackFiles.size)
      val threshold = (1.0d * size / minPathsPerSecond).seconds
      assert(duration < 3.seconds || duration < threshold)
    })
  }

  test("folder list") {
    testRootOpt.foreach(testRoot => {
      Library.setFolders(Seq(testRoot))
      val (size, duration) = Utils.timed(Library.folderStream.size)
      val threshold = (1.0d * size / minPathsPerSecond).seconds
      assert(duration < 3.seconds || duration < threshold)
    })
  }
}
