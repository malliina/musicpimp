package com.malliina.play.http

import java.nio.file.Path

import play.api.mvc.Request

class OneFileUploadRequest[A](val file: Path, user: String, request: Request[A])
  extends CookiedRequest(user, request)
