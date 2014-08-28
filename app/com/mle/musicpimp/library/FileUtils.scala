package com.mle.musicpimp.library

import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

import com.mle.util.Log

/**
 * @author Michael
 */
object FileUtils extends Log {
  /**
   * Non-recursively lists the paths (files and directories) under `dir`.
   *
   * The returned paths are relativized against the supplied root.
   *
   * @param dir parent
   * @return Non-recursive files and dirs.
   */
  def listFiles(dir: Path, root: Path): Folder = {
    val visitor = new MusicItemVisitor(dir, root)
    Files.walkFileTree(dir, visitor)
    Folder(visitor.dirs, visitor.files)
  }

  /**
   * http://stackoverflow.com/a/7264833/1863674
   */
  def fileTree(f: File): Stream[File] =
    f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(fileTree) else Stream.empty)

  def pathTree(path: Path): Stream[Path] = fileTree(path.toFile) map (_.toPath)

  def readableFiles(path: Path) = pathTree(path).filter(p => !Files.isDirectory(p) && Files.isReadable(p))
}

class MusicItemVisitor(val startDir: Path, root: Path)
  extends SimpleFileVisitor[Path] with Log {
  private[this] var dirBuffer: List[Path] = Nil

  private[this] var fileBuffer: List[Path] = Nil

  def dirs = dirBuffer.reverse

  def files = fileBuffer.reverse

  override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
    if (attrs.isRegularFile && file.getFileName.toString.endsWith("mp3")) {
      fileBuffer = preparePath(file) :: fileBuffer
    }
    FileVisitResult.CONTINUE
  }

  override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = {
    if (Files.isSameFile(startDir, dir)) {
      FileVisitResult.CONTINUE
    } else {
      dirBuffer = preparePath(dir) :: dirBuffer
      FileVisitResult.SKIP_SUBTREE
    }
  }

  def preparePath(path: Path) = root relativize path
}
