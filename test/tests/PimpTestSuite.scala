package tests

import java.nio.file.Paths

import com.mle.musicpimp.library.Library
import org.scalatest.FunSuite
import org.scalatestplus.play.OneAppPerSuite

/**
 * @author Michael
 */
trait PimpTestSuite extends FunSuite with OneAppPerSuite {
  val testRoot = Paths get "E:\\musik"
  Library.rootFolders = Seq(testRoot)
  val testFolder = "All+Time+Top+1000"
}
