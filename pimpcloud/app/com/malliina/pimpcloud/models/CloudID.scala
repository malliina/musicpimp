package com.malliina.pimpcloud.models

import com.malliina.musicpimp.models.Identifiable

/**
  * @param id the cloud ID of a connected MusicPimp server
  */
case class CloudID(id: String) extends Identifiable

object CloudID extends Identified[CloudID]
