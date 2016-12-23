package org.musicpimp.js

import org.scalajs.jquery.{JQuery, JQueryEventObject, JQueryStatic, JQueryXHR}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

@js.native
trait AutoItem extends js.Object {
  def id: String = js.native

  def label: String = js.native

  def value: String = js.native
}

object AutoItem {
  def from(t: Track) = {
    val labelAndValue = s"${t.artist} - ${t.title}"
    apply(t.id, labelAndValue, labelAndValue)
  }

  def apply(id: String, label: String, value: String) =
    js.Dynamic.literal(id = id, label = label, value = value).asInstanceOf[AutoItem]
}

@js.native
trait AutoOptions extends js.Object {
  def source: js.Function2[Request, js.Function1[js.Array[AutoItem], js.Any], js.Any] = js.native

  def minLength: Int = js.native

  def select: js.Function2[JQueryEventObject, SelectUi, js.Any] = js.native
}

object AutoOptions {
  def fromAsync(src: Request => Future[Seq[AutoItem]],
                onSelect: AutoItem => Unit,
                minLength: Int = 3) = {
    val source: (Request, Seq[AutoItem] => js.Any) => js.Any =
      (req, callback) => {
        src(req).map(callback) recover {
          // TODO figure out what to do now
          case _ => println("Unable to autocomplete source")
        }
        ()
      }
    val select: (JQueryEventObject, SelectUi) => js.Any =
      (_, ui) => onSelect(ui.item)
    from(source, minLength, select)
  }

  def from(src: (Request, Seq[AutoItem] => js.Any) => js.Any,
           minLength: Int,
           select: (JQueryEventObject, SelectUi) => js.Any) = {
    val source: (Request, js.Function1[js.Array[AutoItem], js.Any]) => js.Any =
      (req, arrayFunc) => {
        val seqFunc: Seq[AutoItem] => js.Any = is => arrayFunc(js.Array(is: _*))
        src(req, seqFunc)
      }
    apply(source, minLength, select)
  }

  def apply(src: js.Function2[Request, js.Function1[js.Array[AutoItem], js.Any], js.Any],
            minLength: Int,
            select: js.Function2[JQueryEventObject, SelectUi, js.Any]): AutoOptions =
    js.Dynamic.literal(source = src, minLength = minLength, select = select).asInstanceOf[AutoOptions]
}

@js.native
trait Request extends js.Object {
  def term: String = js.native
}

object Request {
  def apply(term: String) =
    js.Dynamic.literal(term = term).asInstanceOf[Request]
}

@js.native
trait Response extends js.Object {

}

@js.native
trait SelectUi extends js.Object {
  def item: AutoItem = js.native
}

object SelectUi {
  def apply(item: AutoItem) = {
    ???
  }
}

/**
  * @see http://stackoverflow.com/questions/28065804/implementing-jquery-ui-in-scala-js
  */
@js.native
trait JQueryUI extends JQuery {
  def autocomplete(options: AutoOptions) = js.native
}

object JQueryUI {
  implicit def jQueryExtensions(jQuery: JQuery): JQueryUI =
    jQuery.asInstanceOf[JQueryUI]
}

@js.native
trait PimpQuery extends js.Object {
  def getJSON(url: String,
              data: js.Object,
              success: js.Function3[js.Dynamic, String, JQueryXHR, Any]): JQueryXHR = js.native
}

object PimpQuery {
  implicit def extensions(jQuery: JQueryStatic): PimpQuery =
    jQuery.asInstanceOf[PimpQuery]
}
