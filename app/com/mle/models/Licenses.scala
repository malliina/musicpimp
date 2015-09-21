package com.mle.models

import com.mle.file.FileUtilities

import scala.util.Try

/**
 * @author Michael
 */
object Licenses {
  private val lineSep = sys.props("line.separator")

  val SCALA = fileToString("scala-license.txt")
  val MIT = fileToString("slf4j-mit-license.txt")
  val APACHE = fileToString("LICENSE-2.0.txt")
  val GPL = fileToString("gpl.txt")
  val LGPL = fileToString("lgpl.txt")

  private def fileToString(resource: String) = FileUtilities.readerFrom(s"licenses/$resource")(_.mkString(lineSep))
}
