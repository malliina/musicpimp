package com.mle.musicpimp.library

import java.io.FileNotFoundException
import java.net.{URLDecoder, URLEncoder}
import java.nio.file.{AccessDeniedException, Files, Path, Paths}

import com.mle.audio.meta.SongMeta
import com.mle.file.FileUtilities
import com.mle.musicpimp.db.{DataFolder, DataTrack, PimpDb}
import com.mle.util.{Log, Utils}

/**
 * @author Michael
 */
trait Library extends MusicLibrary with Log {
  val ROOT_ID = ""
  val emptyPath = Paths get ""
  var rootFolders: Seq[Path] = Settings.read

  def rootFolder = rootFolderFromDatabase

  def rootFolderFromDatabase: MusicFolder = folderFromDatabase(ROOT_ID) getOrElse MusicFolder.empty

  def rootFolderFromFile: MusicFolder = MusicFolder.fromFolder(ROOT_ID, emptyPath, rootPaths)

  def folder(id: String): Option[MusicFolder] = {
    //    val (result, duration) = Utils.timed(folderFromDatabase(id))
    //    log info s"Loaded folder: $id in $duration"
    //    result
    folderFromDatabase(id)
  }

  def folderFromDatabase(id: String): Option[MusicFolder] = {
    val path = relativePath(id)
    findPathInfo(path).map(_ => {
      val thisFolder = DataFolder fromPath path
      val (tracks, subFolders) = PimpDb folder id
      MusicFolder(thisFolder, subFolders, tracks)
    })
  }

  def folderFromFile(id: String): Option[MusicFolder] = {
    val path = relativePath(id)
    val content = items(path)
    content.map(MusicFolder.fromFolder(id, path, _))
  }

  private def rootPaths: Folder =
    mergeContents(rootFolders.filter(Files.isDirectory(_)).map(f => PathInfo(emptyPath, f)))

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
    Map(items(root).toSeq.flatMap(f => recurse(f, Map(emptyPath -> f))): _*)
  }

  def all(): Map[Path, Folder] = Map(rootFolders.flatMap(all): _*)

  def allTracks(root: Path) = all(root).map(pair => pair._1 -> pair._2.files)

  def tracksRecursive(root: Path) = allTracks(root).values.flatten

  def songPathsRecursive = all().flatMap(pair => pair._2.files)

  def tracksRecursive: Iterable[LocalTrack] = (songPathsRecursive map findMeta).flatten

  def trackFiles: Stream[Path] = recursivePaths(root => {
    FileUtils.readableFiles(root).filter(_.getFileName.toString endsWith "mp3")
  })

  def tracksStream: Stream[LocalTrack] = trackFiles.distinct map meta

  def folderStream: Stream[DataFolder] = recursivePaths(FileUtils.folders).distinct.map(DataFolder.fromPath)

  private def recursivePaths(rootMap: Path => Stream[Path]) =
    rootFolders.toStream.flatMap(root => rootMap(root).map(root.relativize))

  def dataTrackStream: Stream[DataTrack] = tracksStream map toDataTrack

  def toDataTrack(track: LocalTrack) = {
    val id = track.id
    val path = Option(relativePath(id).getParent) getOrElse emptyPath
    DataTrack(id, track.title, track.artist, track.album, track.duration, track.size, encode(path))
  }

  /**
   * This method has a bug.
   *
   * @param trackId the music item id
   * @return the absolute path to the music item id, or None if no such track exists
   */
  def findAbsolute(trackId: String): Option[Path] = findPathInfo(relativePath(trackId)).map(_.absolute)

  def suggestAbsolute(relative: Path): Option[Path] = rootFolders.headOption.map(_ resolve relative)

  def suggestAbsolute(path: String): Option[Path] = suggestAbsolute(relativePath(path))

  def relativePath(itemId: String): Path = Paths get decode(itemId)

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

  def decode(id: String) = URLDecoder.decode(id, "UTF-8")

  private def items(relative: Path): Option[Folder] = {
    val sources = findPathInfo2(relative)
    if (sources.isEmpty) None
    else Some(mergeContents(sources))
  }

  private def mergeContents(sources: Seq[PathInfo]): Folder = sources.map(items).foldLeft(Folder.empty)(_ ++ _)

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

  // How do I write this correctly?
  //  private def yo[R[_]](f: Seq[Path] => R[PathInfo]) = f(rootFolders).map(PathInfo(relative,_))

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

