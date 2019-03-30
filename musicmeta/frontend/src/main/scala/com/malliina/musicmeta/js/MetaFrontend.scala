package com.malliina.musicmeta.js

import org.scalajs.jquery.JQueryStatic

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object MetaFrontend {
  private val jq = MyJQuery
  private val p = Popper
  private val b = Bootstrap

  def main(args: Array[String]): Unit = {
    new MetaSocket
  }
}

@js.native
@JSImport("jquery", JSImport.Namespace)
object MyJQuery extends JQueryStatic

@js.native
@JSImport("popper.js", JSImport.Namespace)
object Popper extends js.Object

@js.native
@JSImport("bootstrap", JSImport.Namespace)
object Bootstrap extends js.Object
