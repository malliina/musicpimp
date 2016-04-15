package com.malliina.musicpimp.scheduler

abstract class BaseEnumeration extends Enumeration {
  def withNameIgnoreCase(name: String) = values.find(_.toString.toLowerCase == name.toLowerCase)
    .getOrElse(throw new NoSuchElementException(s"Unknown enumeration name: $name"))

  implicit object jsonFromat extends JsonHelpers.SimpleFormat[Value](withNameIgnoreCase)

}
