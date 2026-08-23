package com.malliina.http4s

object PimpExt extends PimpExt

trait PimpExt:
  extension [L, R](e: Either[L, R]) def handleLeft[S >: R](code: L => S): S = e.fold(code, identity)
