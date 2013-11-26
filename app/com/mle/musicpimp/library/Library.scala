package com.mle.musicpimp.library

import com.mle.util.{Util, Log}
import java.nio.file.{AccessDeniedException, Paths, Files, Path}
import java.net.{URLEncoder, URLDecoder}
import com.mle.audio.meta.SongMeta
import java.io.FileNotFoundException

/**
 * An item is either a song or a folder.
 *
 * @author Michael
 */
trait Library extends Log {
  var rootFolders: Seq[Path] = Settings.read

  //  def loadLibrary: Map[String, Folder] = {
  //    val root = musicItems
  //    root.dirs.foldLeft(Map("" -> root))((acc, subDir) => acc ++ subFolders(subDir))
  //  }

  // recursive
  //  private def subFolders(dir: Path): Map[String, Folder] = {
  //    val items = itemsInRelative(dir)
  //    items.dirs.foldLeft(Map(encode(dir) -> items))((acc, subDir) => acc ++ subFolders(subDir))
  //  }

  def items(relative: Path): Option[Folder] =
    findPathInfo(relative).map(items)

  def items(pathInfo: PathInfo): Folder =
    tryReadFolder(FileUtils.listFiles(pathInfo.absolute, pathInfo.root))

  def musicItems =
    rootFolders
      .filter(Files.isDirectory(_))
      .map(f => tryReadFolder(FileUtils.listFiles(f, f)))
      .foldLeft(Folder.empty)(_ ++ _)

  def findPathInfo(relative: Path): Option[PathInfo] = {
    rootFolders.find(root => Files.exists(root resolve relative)).map(root => {
      PathInfo(relative, root)
    })
  }

  def pathInfo(relative: Path): PathInfo = findPathInfo(relative)
    .getOrElse(throw new FileNotFoundException(s"Root folder for $relative not found."))

  /**
   * Some folders might have unsuitable permissions, throwing an exception
   * when a read attempt is made. Suppresses such AccessDeniedExceptions.
   *
   * @param f function that returns folder contents
   * @return the folder, or an empty folder if the folder could not be read
   */
  private def tryReadFolder(f: => Folder): Folder =
    Util.optionally[Folder, AccessDeniedException](f).getOrElse(Folder.empty)

  /**
   * This method has a bug.
   *
   * @param trackId the music item id
   * @return the absolute path to the music item id
   */
  def toAbsolute(trackId: String): Option[Path] =
    findPathInfo(relativePath(trackId)).map(_.absolute)

  def relativePath(itemId: String) = {
    val decodedId = URLDecoder.decode(itemId, "UTF-8")
    Paths get decodedId
  }

  def metaFor(song: Path): TrackInfo = {
    val pathData = pathInfo(song)
    val meta = SongMeta.fromPath(pathData.absolute, pathData.root)
    new TrackInfo(encode(song), meta)
  }

  def metaFor(itemId: String): TrackInfo = Library metaFor relativePath(itemId)

  /**
   * Generates a URL-safe ID of the given music item.
   *
   * TODO: make item unique
   *
   * @param path path to music file or folder
   * @return the id
   */
  def encode(path: Path) = URLEncoder.encode(path.toString, "UTF-8")
}

object Library extends Library

case class PathInfo(relative: Path, root: Path) {
  def absolute = root resolve relative
}

