package com.malliina.musicpimp.http

import java.nio.file.Path

import com.malliina.file.FileUtilities
import com.malliina.play.controllers.Uploads
import play.api.libs.{Files => PlayFiles}
import play.api.mvc.{MultipartFormData, Request}

object PimpUploads extends Uploads(FileUtilities.tempDir) {
  def save(request: Request[MultipartFormData[PlayFiles.TemporaryFile]]): Seq[Path] =
    saveFiles(request)
}
