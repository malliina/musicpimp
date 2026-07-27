package com.malliina.musicpimp.models

import com.malliina.values.ErrorMessage

object CloudIDs extends IDCompanion[CloudID]:
  override def build(input: String): Either[ErrorMessage, CloudID] = Right(apply(input))
  def apply(raw: String): CloudID = CloudID(raw)
