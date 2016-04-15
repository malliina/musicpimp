package tests

import java.nio.file.Paths

import com.malliina.util.Log
import org.java_websocket.util.Base64
import org.scalatest.FunSuite
import play.api.libs.json.Json

class Tests extends FunSuite with Log {
  //  val testPath = Paths get ""
  test("can run test") {

  }
  test("stream of paths") {
    //    FileUtils.pathTree(testPath).foreach(println)
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
    import play.api.libs.json.Json._
    val jsValue = stringify(toJson(Some(42)))
    val none = stringify(toJson(Option.empty[Int]))
    assert(jsValue === "42")
    assert(none === "null")
  }
//  test("base64") {
//    val username = "test"
//    val password = "test"
//    val value = Base64.encodeBytes((username + ":" + password).getBytes("UTF-8"))
//    println(value)
//  }
}
