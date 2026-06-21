package com.malliina.musicpimp.app

import com.malliina.config.ConfigReadable
import com.malliina.musicpimp.BuildInfo
import com.malliina.values.ErrorMessage

enum AppMode:
  case Prod
  case Dev
  def isProd = this == Prod

object AppMode:
  val fromBuild: AppMode = if BuildInfo.isProd then Prod else Dev

  given ConfigReadable[AppMode] = ConfigReadable.string.emapParsed: s =>
    fromString(s)

  def unsafe(in: String): AppMode =
    fromString(in).fold(err => throw new IllegalArgumentException(err.message), identity)

  def fromString(in: String): Either[ErrorMessage, AppMode] = in match
    case "prod" => Right(Prod)
    case "dev"  => Right(Dev)
    case other  => Left(ErrorMessage(s"Invalid mode: '$other'. Must be 'prod' or 'dev'."))
