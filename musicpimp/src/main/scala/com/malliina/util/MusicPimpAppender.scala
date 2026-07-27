package com.malliina.util

import cats.effect.Async
import com.malliina.logback.fs2.{DefaultFS2IOAppender, LoggingComps}

class MusicPimpAppender[F[_]: Async](comps: LoggingComps[F]) extends DefaultFS2IOAppender[F](comps)
