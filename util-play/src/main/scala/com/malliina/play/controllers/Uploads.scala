package com.malliina.play.controllers

import java.nio.file.{Files, Path}

import play.api.libs.Files as PlayFiles
import play.api.mvc.{MultipartFormData, Request}

class Uploads(uploadDir: Path):
  def saveFiles(request: Request[MultipartFormData[PlayFiles.TemporaryFile]]): Seq[Path] =
    request.body.files.map: file =>
      val dest = uploadDir.resolve(file.filename)
      if !Files.exists(dest) then file.ref.moveTo(dest, replace = true)
      dest
