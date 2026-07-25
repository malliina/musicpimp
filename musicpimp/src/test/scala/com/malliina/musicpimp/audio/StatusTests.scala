package com.malliina.musicpimp.audio

class StatusTests extends munit.FunSuite:
  val json =
    """{"track":{"id":"782db0e7fa7d1f029303e1e9d2d99d80","title":"Desire","artist":"Ryan Adams","album":"48 Hours","path":"ryan adams - desire.mp3","duration":231.0,"size":5546654,"url":"http:///downloads/782db0e7fa7d1f029303e1e9d2d99d80"},"state":"Started","position":5.0,"volume":40,"mute":false,"playlist":[{"id":"782db0e7fa7d1f029303e1e9d2d99d80","title":"Desire","artist":"Ryan Adams","album":"48 Hours","path":"ryan adams - desire.mp3","duration":231.0,"size":5546654,"url":"http:///downloads/782db0e7fa7d1f029303e1e9d2d99d80"}],"index":0,"event":"status"}"""

  test("parse status"):
    import io.circe.parser
    val res = parser.decode[StatusMessage](json)
    assert(res.isLeft)
    println(res)
