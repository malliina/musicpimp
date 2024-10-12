package com.malliina.play.http

import java.nio.file.Path

import play.api.mvc.Request

class FileUploadRequest[A, U](val files: Seq[Path], user: U, request: Request[A])
  extends CookiedRequest(user, request)
