package tests

import com.malliina.musicpimp.models.RequestID
import org.scalatest.FunSuite

class ModelTests extends FunSuite {
  test("RequestID validation") {
    val bogus = RequestID.build("")
    assert(bogus.isEmpty)
  }

  test("model validation and equality") {
    // Does not compile
    //    RequestID("123")
    val id1 = RequestID.build("123").get
    val id2 = RequestID.build("123").get
    assert(id1 === id2)

  }
}
