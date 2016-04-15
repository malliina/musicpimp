package com.malliina.util

import com.malliina.storage.{StorageLong, StorageSize}

object Platform {
  val availableJvmMemory: StorageSize = Runtime.getRuntime.maxMemory().bytes
  val platform: Architecture = sys.props.get("os.arch") map fromName getOrElse Unknown

  def fromName(name: String) = name match {
    case "arm" => ARM
    case "amd64" => AMD64
    case other => Other(other)
  }

  trait Architecture

  case object ARM extends Architecture

  case object AMD64 extends Architecture

  case class Other(name: String) extends Architecture

  case object Unknown extends Architecture

}