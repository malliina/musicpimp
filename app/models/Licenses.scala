package models

import com.mle.util.FileUtilities

/**
 * @author Michael
 */
object Licenses {
  private val lineSep = sys.props("line.separator")

  private def fileToString(resource: String) = FileUtilities.readerFrom(resource)(_.mkString(lineSep))

  val SCALA = fileToString("licenses/scala-license.txt")
  val MIT = fileToString("licenses/slf4j-mit-license.txt")
  val APACHE = fileToString("licenses/LICENSE-2.0.txt")
  val GPL = fileToString("licenses/gpl.txt")
  val LGPL = fileToString("licenses/lgpl.txt")
}
