package com.malliina.musicpimp.app

import com.malliina.database.Conf
import com.malliina.http.FullUrl
import com.malliina.musicpimp.cloud.CloudSocket

import java.security.SecureRandom

case class InitOptions(
  alarms: Boolean = true,
  users: Boolean = true,
  indexer: Boolean = true,
  cloud: Boolean = true,
  cloudUri: FullUrl = CloudSocket.devUri,
  useTray: Boolean = true
)

object InitOptions:
  val prod = InitOptions()
  val dev = InitOptions(
    alarms = false,
    users = true,
    indexer = true,
    cloud = false,
    useTray = false
  )

  // Ripped from Play's ApplicationSecretGenerator.scala
  def generateSecret(): String =
    val random = new SecureRandom()
    (1 to 64)
      .map: _ =>
        (random.nextInt(75) + 48).toChar
      .mkString
      .replaceAll("\\\\+", "/")

trait AppConf:
  def databaseConf: Conf
  def close(): Unit
