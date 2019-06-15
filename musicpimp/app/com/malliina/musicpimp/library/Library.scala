package com.malliina.musicpimp.library

import java.io.FileNotFoundException
import java.nio.file.{AccessDeniedException, Files, Path, Paths}

import com.malliina.audio.meta.SongMeta
import com.malliina.musicpimp.audio.{PimpEnc, TrackMeta}
import com.malliina.musicpimp.db._
import com.malliina.musicpimp.library.Library._
import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.util.Utils
import com.malliina.values.UnixPath
import org.apache.commons.codec.digest.DigestUtils
import play.api.Logger

import scala.concurrent.stm.{Ref, atomic}

object Library extends Library {
  private val log = Logger(getClass)

  val RootId = FolderID(idFor(""))
  val EmptyPath = Paths get ""

  def idFor(in: String) = DigestUtils.md5Hex(in)

  def parent(p: Path) = folderId(Option(p.getParent).getOrElse(EmptyPath))

  def folderId(p: Path) = FolderID(idFor(UnixPath(p).path))

  def trackId(p: Path) = TrackID(idFor(UnixPath(p).path))
}

class Library {
  private val rootFolders: Ref[Seq[Path]] = Ref(Settings.read)

  private def roots = rootFolders.single.get

  private def rootStream = roots.toStream

  def reloadFolders(): Unit = setFolders(Settings.read)

  def setFolders(folders: Seq[Path]): Unit = atomic(txn => rootFolders.set(folders)(txn))

  def localize(tracks: Seq[TrackMeta]) = tracks.flatMap(track => findMeta(track.relativePath))

  private def all(root: Path): Map[Path, Folder] = {
    def recurse(folder: Folder, acc: Map[Path, Folder]): Map[Path, Folder] = {
      if (folder.dirs.isEmpty) {
        acc
      } else {
        Map(folder.dirs
          .flatMap(dir => items(dir).toSeq
            .flatMap(f => recurse(f, acc.updated(dir, f)))): _*)
      }
    }

    Map(items(root).toSeq.flatMap(f => recurse(f, Map(EmptyPath -> f))): _*)
  }

  private def all(): Map[Path, Folder] = Map(roots.flatMap(all): _*)

  def songPathsRecursive = all().flatMap(pair => pair._2.files)

  def tracksRecursive: Iterable[LocalTrack] = (songPathsRecursive map findMeta).flatten

  def trackFiles: Stream[Path] = recursivePaths(audioFiles)

  private def tracksStream: Stream[LocalTrack] = (tracksPathInfo.distinct map parseMeta).flatten

  private def tracksPathInfo = rootStream.flatMap(root => audioFiles(root).map(f => PathInfo(root.relativize(f), root)))

  private def audioFiles(root: Path) = FileUtils.readableFiles(root).filter(_.getFileName.toString endsWith "mp3")

  def folderStream: Stream[DataFolder] = recursivePaths(FileUtils.folders).distinct.map(DataFolder.fromPath)

  private def recursivePaths(rootMap: Path => Stream[Path]) =
    rootStream.flatMap(root => rootMap(root).map(root.relativize))

  def dataTrackStream: Stream[DataTrack] = tracksStream map toDataTrack

  private def toDataTrack(track: LocalTrack) = {
    val id = track.id
    val parent = Option(track.relativePath.getParent).map(p => FolderID(idFor(UnixPath(p).path))) getOrElse RootId
    DataTrack(id, track.title, track.artist, track.album, track.duration, track.size, track.path, parent)
  }

  /** This method has a bug.
    *
    * @param trackId the music item id
    * @return the absolute path to the music item id, or None if no such track exists
    */
  def findAbsoluteLegacy(trackId: TrackID): Option[Path] = findPathInfo(Paths.get(PimpEnc.decodeId(trackId))).map(_.absolute)

  def findAbsoluteNew(trackPath: UnixPath): Option[Path] = findPathInfo(Paths.get(trackPath.path)).map(_.absolute)

  def suggestAbsolute(relative: Path): Option[Path] = roots.headOption.map(_ resolve relative)

  //  def suggestAbsolute(path: TrackID): Option[Path] = suggestAbsolute(relativePath(path))

  //  private def meta(itemId: TrackID): LocalTrack = meta(relativePath(itemId))

  private def meta(relative: Path): LocalTrack =
    localTrackFor(pathInfo(relative))

  def localTrackFor(pi: PathInfo) = {
    val meta = SongMeta.fromPath(pi.absolute, pi.root)
    val path = UnixPath(pi.relative)
    new LocalTrack(TrackID(idFor(path.path)), path, meta)
  }

  def toLocal(track: TrackMeta) = meta(track.relativePath)

  //  def findMeta(id: TrackID): Option[LocalTrack] = findMeta(relativePath(id))

  private def findMeta(relative: Path): Option[LocalTrack] = findPathInfo(relative) flatMap parseMeta

  private def parseMeta(pi: PathInfo): Option[LocalTrack] =
    try {
      // InvalidAudioFrameException, CannotReadException
      Option(localTrackFor(pi))
    } catch {
      case e: Exception =>
        log.debug(s"Unable to read file: ${pi.absolute}. The file will be excluded from the library.", e)
        None
    }

  private def items(relative: Path): Option[Folder] = {
    val sources = findPathInfo2(relative)
    if (sources.isEmpty) None
    else Some(mergeContents(sources))
  }

  private def mergeContents(sources: Seq[PathInfo]): Folder = sources.map(items).foldLeft(Folder.empty)(_ ++ _)

  private def items(pathInfo: PathInfo): Folder =
    tryReadFolder(FileUtils.listFiles(pathInfo.absolute, pathInfo.root))

  private def findPathInfo(relative: Path): Option[PathInfo] = {
    roots.find(root => Files.isReadable(root resolve relative))
      .map(root => PathInfo(relative, root))
  }

  private def findPathInfo2(relative: Path): Seq[PathInfo] = {
    roots.filter(root => Files.isReadable(root resolve relative))
      .map(root => PathInfo(relative, root))
  }

  private def pathInfo(relative: Path): PathInfo = findPathInfo(relative)
    .getOrElse(throw new FileNotFoundException(s"Root folder for $relative not found."))

  /** Some folders might have unsuitable permissions, throwing an exception when a read attempt is made. Suppresses such
    * AccessDeniedExceptions.
    *
    * @param f function that returns folder contents
    * @return the folder, or an empty folder if the folder could not be read
    */
  private def tryReadFolder(f: => Folder): Folder =
    Utils.opt[Folder, AccessDeniedException](f) getOrElse Folder.empty
}

case class PathInfo(relative: Path, root: Path) {
  def absolute = root resolve relative
}
