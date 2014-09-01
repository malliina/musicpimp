package com.mle.musicpimp.library

import java.io.FileNotFoundException
import java.net.{URLDecoder, URLEncoder}
import java.nio.file.{AccessDeniedException, Files, Path, Paths}

import com.mle.audio.meta.SongMeta
import com.mle.musicpimp.db.DataTrack
import com.mle.util.{FileUtilities, Log, Utils}

/**
 * An item is either a song or a folder.
 *
 * @author Michael
 */
trait Library extends MusicLibrary with Log {
  var rootFolders: Seq[Path] = Settings.read

  def rootItems: Folder =
    mergeContents(
      rootFolders
        .filter(Files.isDirectory(_))
        .map(f => PathInfo(Paths get "", f)))

  def items(relative: Path): Option[Folder] = {
    val sources = findPathInfo2(relative)
    if (sources.isEmpty) {
      None
    } else {
      Some(mergeContents(sources))
    }
  }

  def all(root: Path): Map[Path, Folder] = {
    def recurse(folder: Folder, acc: Map[Path, Folder]): Map[Path, Folder] = {
      if (folder.dirs.isEmpty) {
        acc
      } else {
        Map(folder.dirs
          .flatMap(dir => items(dir).toSeq
          .flatMap(f => recurse(f, acc.updated(dir, f)))): _*)
      }
    }
    def recurse2(folder: Folder, acc: Map[Path, Folder]): Map[Path, Folder] = {
      if (folder.dirs.isEmpty) {
        acc
      } else {
        Map((for {
          dir <- folder.dirs
          subFolder <- items(dir).toSeq
          pair <- recurse2(subFolder, acc.updated(dir, subFolder))
        } yield pair): _*)
      }
    }
    Map(items(root).toSeq.flatMap(f => recurse(f, Map(Paths.get("") -> f))): _*)
  }

  def all(): Map[Path, Folder] = Map(rootFolders.flatMap(all): _*)

  def allTracks(root: Path) = all(root).map(pair => pair._1 -> pair._2.files)

  def tracksRecursive(root: Path) = allTracks(root).values.flatten

  def songPathsRecursive = all().flatMap(pair => pair._2.files)

  def tracksRecursive: Iterable[LocalTrack] = (songPathsRecursive map findMeta).flatten

  def trackFiles: Stream[Path] = rootFolders.toStream
    .flatMap(root => FileUtils.readableFiles(root).filter(_.getFileName.toString endsWith "mp3").map(root.relativize))

  def tracksStream: Stream[LocalTrack] = trackFiles map meta

  def dataTrackStream: Stream[DataTrack] = tracksStream map toDataTrack

  def toDataTrack(track: LocalTrack) = DataTrack(track.id, track.title, track.artist, track.album, track.duration, track.size)

  /**
   * This method has a bug.
   *
   * @param trackId the music item id
   * @return the absolute path to the music item id, or None if no such track exists
   */
  def findAbsolute(trackId: String): Option[Path] =
    findPathInfo(relativePath(trackId)).map(_.absolute)

  def suggestAbsolute(relative: Path): Option[Path] = rootFolders.headOption.map(_ resolve relative)

  def suggestAbsolute(path: String): Option[Path] = suggestAbsolute(relativePath(path))

  def relativePath(itemId: String): Path = {
    val decodedId = URLDecoder.decode(itemId, "UTF-8")
    Paths get decodedId
  }

  def meta(song: Path): LocalTrack = {
    val pathData = pathInfo(song)
    val meta = SongMeta.fromPath(pathData.absolute, pathData.root)
    new LocalTrack(encode(song), meta)
  }

  def meta(itemId: String): LocalTrack = Library meta relativePath(itemId)

  def findMeta(relative: Path): Option[LocalTrack] = findPathInfo(relative).flatMap(parseMeta)

  def findMeta(id: String): Option[LocalTrack] = findMeta(relativePath(id))

  def parseMeta(relative: Path, root: Path): Option[LocalTrack] = parseMeta(PathInfo(relative, root))

  def parseMeta(pi: PathInfo): Option[LocalTrack] =
    Utils.opt[LocalTrack, Exception] {
      // InvalidAudioFrameException, CannotReadException
      val meta = SongMeta.fromPath(pi.absolute, pi.root)
      new LocalTrack(encode(pi.relative), meta)
    }

  def findMetaWithTempFallback(id: String) = findMeta(id).orElse(searchTempDir(id))

  def searchTempDir(id: String): Option[LocalTrack] = {
    val pathInfo = PathInfo(Library.relativePath(id), FileUtilities.tempDir)
    val absolute = pathInfo.absolute
    if (Files.exists(absolute) && Files.isReadable(absolute)) Library.parseMeta(pathInfo)
    else None
  }

  /**
   * Generates a URL-safe ID of the given music item.
   *
   * TODO: make item unique
   *
   * @param path path to music file or folder
   * @return the id
   */
  def encode(path: Path) = URLEncoder.encode(path.toString, "UTF-8")

  private def mergeContents(sources: Seq[PathInfo]): Folder =
    sources.map(items).foldLeft(Folder.empty)(_ ++ _)

  private def items(pathInfo: PathInfo): Folder =
    tryReadFolder(FileUtils.listFiles(pathInfo.absolute, pathInfo.root))

  private def findPathInfo(relative: Path): Option[PathInfo] = {
    rootFolders.find(root => Files.isReadable(root resolve relative))
      .map(root => PathInfo(relative, root))
  }

  private def findPathInfo2(relative: Path): Seq[PathInfo] = {
    rootFolders.filter(root => Files.isReadable(root resolve relative))
      .map(root => PathInfo(relative, root))
  }

  private def pathInfo(relative: Path): PathInfo = findPathInfo(relative)
    .getOrElse(throw new FileNotFoundException(s"Root folder for $relative not found."))

  /**
   * Some folders might have unsuitable permissions, throwing an exception when a read attempt is made. Suppresses such
   * AccessDeniedExceptions.
   *
   * @param f function that returns folder contents
   * @return the folder, or an empty folder if the folder could not be read
   */
  private def tryReadFolder(f: => Folder): Folder =
    Utils.opt[Folder, AccessDeniedException](f).getOrElse(Folder.empty)
}

object Library extends Library

case class PathInfo(relative: Path, root: Path) {
  def absolute = root resolve relative
}

