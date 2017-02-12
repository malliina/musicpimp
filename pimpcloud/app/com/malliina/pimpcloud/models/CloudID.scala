package com.malliina.pimpcloud.models

/**
  * @param id the cloud ID of a connected MusicPimp server
  */
case class CloudID(id: String) extends Identifiable

object CloudID extends Identified[CloudID]
