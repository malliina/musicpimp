package com.mle.models

import com.mle.file.FileUtilities

import scala.util.Try

/**
 * @author Michael
 */
object Licenses {
  private val lineSep = sys.props("line.separator")

  val SCALA = fileResiliently("scala-license.txt")
  val MIT = fileResiliently("slf4j-mit-license.txt")
  val APACHE = fileResiliently("LICENSE-2.0.txt")
  val GPL = fileResiliently("gpl.txt")
  val LGPL = fileResiliently("lgpl.txt")

  // hack due to debian packaging issues
  private def fileResiliently(name: String) = Try(fileToString(s"licenses/$name")).getOrElse(fileToString(s"conf/licenses/$name"))

  private def fileToString(resource: String) = FileUtilities.readerFrom(resource)(_.mkString(lineSep))
}
