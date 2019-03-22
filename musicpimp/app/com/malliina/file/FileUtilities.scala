package com.malliina.file

import java.io.{BufferedWriter, FileNotFoundException, FileWriter, PrintWriter}
import java.nio.file._

import com.malliina.file.FileVisitors.FileCollectingVisitor
import com.malliina.util.{Util, Utils}
import org.apache.commons.io.IOUtils

import scala.io.Source

object FileUtilities {
  val lineSep = sys.props("line.separator")
  val userDirString = sys.props("user.dir")
  val userDir = Paths get userDirString
  val userHome = Paths get sys.props("user.home")
  val tempDir = Paths get sys.props("java.io.tmpdir")
  var basePath = Paths get sys.props.getOrElse("app.home", userDirString)

  def init(appName: String) {
    basePath = Paths get sys.props.get(appName + ".home").getOrElse(userDirString)
  }

  def pathTo(location: String) = basePath / location

  def listFiles(srcDir: Path, visitor: FileCollectingVisitor): Seq[Path] = {
    //    log debug "Reading " + srcDir.toAbsolutePath.toString
    Files.walkFileTree(srcDir, visitor)
    visitor.files
  }

  /**
    * @see <a href="http://stackoverflow.com/a/4608061">http://stackoverflow.com/a/4608061</a>
    * @param filename the file to write to
    * @param op       the file writing code
    */
  def writerTo(filename: String)(op: PrintWriter => Unit): Path = {
    val path = pathTo(filename)
    writerTo(path)(op)
    path
  }

  /**
    * @see <a href="http://stackoverflow.com/a/4608061">http://stackoverflow.com/a/4608061</a>
    * @param filename the file to write to
    * @param op       the file writing code
    */
  def writerTo(filename: Path)(op: PrintWriter => Unit): Unit =
    Util.using(new PrintWriter(new BufferedWriter(new FileWriter(filename.toFile))))(op)

  def readerFrom[T](path: Path)(code: Iterator[String] => T): T =
    Utils.resource(Source.fromFile(path.toFile)) {
      source => code(source.getLines())
    }

  /** Throws if the file doesn't exist/has no first line (?)
    *
    * @param path location of file
    * @return the first line of the file at the specified location
    */
  def firstLine(path: Path) = readerFrom(path)(_.next())

  /** Avoids io.Source.fromURI(uri) because it seems to fail unless the supplied URI points to a file.
    *
    * @param resource
    * @param code
    * @tparam T
    * @return
    */
  def readerFrom[T](resource: String)(code: Iterator[String] => T): T = {
    val maybeFile = FileUtilities pathTo resource
    if (Files exists maybeFile) {
      readerFrom(maybeFile)(code)
    } else {
      Util.using(Util.openStream(resource))(inStream => {
        Utils.resource(Source.fromInputStream(inStream))(source => code(source.getLines()))
      })
    }
  }

  def fileToString(file: Path): String =
    readerFrom(file)(_.mkString(lineSep))

  def stringToFile(str: String, file: Path) =
    writerTo(file)(_.println(str))

  /**
    * Calculates the amount of used disk space, in percentages, according to the formula: usable_space / total_space. For example, if a 10 GB disk contains 3 GB of data, this method returns 30 for that disk.
    *
    * @param path the path to the disk or file store
    * @return the amount of used disk space as a percentage [0,100] of the total disk space capacity, rounded up to the next integer
    */
  def diskUsagePercentage(path: Path) = {
    val fileStore = Files getFileStore path
    val totalSpace = fileStore.getTotalSpace
    val freeSpace = fileStore.getUsableSpace
    math.ceil(100.0 * freeSpace / totalSpace).toInt
  }

  /**
    * Copies the given files to a destination directory, where the files' subdirectory is calculated relative to the given base directory.
    *
    * @param srcBase the base directory for the source files
    * @param files   the source files to copy
    * @param dest    the destination directory, so each source file is copied to <code>dest / srcBase.relativize(file)</code>
    * @return the destination files
    */
  def copy(srcBase: Path, files: Set[Path], dest: Path) = files map (file => {
    val destFile = rebase(file, srcBase, dest)
    // Create parent dirs if they don't exist
    val parentDir = destFile.getParent
    if (parentDir != null && !Files.isDirectory(parentDir))
      Files createDirectories parentDir
    // Target directory guaranteed to exist, so copy the target, unless it is a directory that already exists
    if (!Files.isDirectory(destFile))
      Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING)
    else destFile
  })

  def rebase(file: Path, srcBase: Path, destBase: Path) = destBase resolve (srcBase relativize file)

  /**
    * Performs a recursive search of files and directories under the given base path.
    *
    * @param basePath the base directory
    * @return The files and directories under the base directory. Directories precede any files they contain in the returned sequence.
    */
  def listPaths(basePath: Path): Seq[Path] = {
    val visitor = new FileVisitors.FileAndDirCollector
    Files walkFileTree(basePath, visitor)
    visitor.files
  }

  /**
    * Creates the file referenced by the specified path and any non-existing parent directories. No-ops if the file already exists.
    *
    * @param path the path to the file to create
    * @see [[java.nio.file.Files]], [[java.nio.file.Paths]]
    */
  def createFile(path: String) {
    val file = pathTo(path)
    if (!Files.exists(file)) {
      val maybeParent = file.getParent
      if (maybeParent != null)
        Files createDirectories file.getParent
      Files createFile file
    }
  }

  def propsToFile(props: String*) = {
    props flatMap (prop => resourceToFile(sys.props(prop)))
  }

  /**
    *
    * @param resource the resource to lookup and write to file
    * @return the path wrapped in an option if it was written, None if no file was written because it already existed
    */
  def resourceToFile(resource: String): Option[Path] = {
    val destFile = pathTo(resource)
    if (!Files.exists(destFile)) {
      val url = Util resource resource
      val bytes = IOUtils.toByteArray(url)
      Files.createDirectories(destFile.getParent)
      Some(Files.write(destFile, bytes))
    } else {
      None
    }
  }

  def verifyFileReadability(file: Path): Unit = {
    import Files._
    if (!exists(file)) {
      throw new FileNotFoundException(file.toString)
    }
    if (!isRegularFile(file)) {
      throw new Exception(s"Not a regular file: $file")
    }
    if (!isReadable(file)) {
      throw new Exception(s"File exists but is not readable: $file")
    }
  }

}
