package tests

import com.mle.util.{FileUtilities, Log}
import org.scalatest.FunSuite

import play.api.libs.json.Json
import java.nio.file.Paths
import com.mle.musicpimp.library.Library

/**
 * @author Michael
 */
class Tests extends FunSuite with Log {
  test("can run test") {

  }
  test("paths") {
    val root = Paths get "a/b/c"
    val rel = Paths get ""
    val combined = root resolve rel
    assert(root.toAbsolutePath.toString === combined.toAbsolutePath.toString)
  }
  test("deconstruct array") {
    val arr = "a:b".split(":")
    arr match {
      case Array(a, b) => assert(a === "a")
      case _ => assert(1 === 2)
    }
  }
  test("for comp.") {
    def eval(in: String) = {
      def isA(input: String) = input == "a"
      val maybeV = Some(in)
      for (actual <- maybeV if isA(actual)) yield actual
    }
    assert(eval("a") === Some("a"))
    assert(eval("b") === None)
  }
  test("json") {
    val in = Seq("a", "b", "c")
    val jsV = Json.toJson(Map("folders" -> in))
    val jsString = Json.stringify(jsV)
    val readV = Json.parse(jsString)
    val list = (readV \ "folders").as[Seq[String]]
    assert(in === list)
  }
  test("serialize Option") {
    import Json._
    val jsValue = stringify(toJson(Some(42)))
    val none = stringify(toJson(Option.empty[Int]))
    assert(jsValue === "42")
    assert(none === "null")
  }
}
