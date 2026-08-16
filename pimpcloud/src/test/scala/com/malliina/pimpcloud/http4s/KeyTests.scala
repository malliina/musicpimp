package com.malliina.pimpcloud.http4s

import com.malliina.play.ContentRange
import com.malliina.storage.StorageInt
import com.malliina.web.Utils
import org.http4s.Header
import org.http4s.headers.Range.SubRange
import org.http4s.headers.{`Content-Disposition`, `Content-Range`}
import org.typelevel.ci.CIStringSyntax

class KeyTests extends munit.FunSuite:
  test("hey"):
    println(Utils.randomString())

  test("cd"):
    val h = org.http4s.headers.`Content-Disposition`("inline", Map(ci"filename" -> "music.mp3"))
    println(h)
    println(`Content-Disposition`.headerInstance.value(h))
    val hr: Header.ToRaw = h

  test("ct"):
    val size = 100.bytes
    val range = ContentRange(0, 10, size)
    val cr = `Content-Range`(SubRange(range.start, range.endInclusive), Option(size.toBytes))
    val str = `Content-Range`.headerInstance.value(cr)
    assertEquals(range.contentRange, str)
