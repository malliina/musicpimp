package com.malliina.musicpimp.library

import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

object FileUtils:

  /** Non-recursively lists the paths (files and directories) under `dir`.
    *
    * The returned paths are relativized against the supplied root.
    *
    * @param dir
    *   parent
    * @return
    *   Non-recursive files and dirs.
    */
  def listFiles(dir: Path, root: Path): Folder =
    val visitor = new MusicItemVisitor(dir, root)
    Files.walkFileTree(dir, visitor)
    Folder(visitor.dirs, visitor.files)

  /** '#::' is cons for [[LazyList]]s
    *
    * @see
    *   http://stackoverflow.com/a/7264833/1863674
    */
  def fileTree(f: File): LazyList[File] =
    f #:: (if f.isDirectory then LazyList.from(f.listFiles()).flatMap(fileTree) else LazyList.empty)

  def pathTree(path: Path): LazyList[Path] = fileTree(path.toFile) map (_.toPath)

  def folders(path: Path): LazyList[Path] = pathTree(path).filter(Files.isDirectory(_))

  def readableFiles(path: Path) =
    pathTree(path).filter(p => !Files.isDirectory(p) && Files.isReadable(p))

class MusicItemVisitor(val startDir: Path, root: Path) extends SimpleFileVisitor[Path]:
  private var dirBuffer: List[Path] = Nil

  private var fileBuffer: List[Path] = Nil

  def dirs = dirBuffer.reverse

  def files = fileBuffer.reverse

  override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult =
    if attrs.isRegularFile && file.getFileName.toString.endsWith("mp3") then
      fileBuffer = preparePath(file) :: fileBuffer
    FileVisitResult.CONTINUE

  override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) =
    if Files.isSameFile(startDir, dir) then FileVisitResult.CONTINUE
    else
      dirBuffer = preparePath(dir) :: dirBuffer
      FileVisitResult.SKIP_SUBTREE

  def preparePath(path: Path) = root.relativize(path)
