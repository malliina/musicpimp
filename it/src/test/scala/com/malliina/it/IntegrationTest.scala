package com.malliina.it

import org.scalatest.FunSuite
import play.api.Application
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests.{MusicPimpSuite, PimpcloudSuite}

class IntegrationTest extends FunSuite {
  val musicpimp = new MusicPimpSuite
  val pimpcloud = new PimpcloudSuite

  test("can do it") {
    assert(statusCode("/ping", musicpimp.app) === 200)
    assert(statusCode("/health", pimpcloud.app) === 200)
  }

  def statusCode(uri: String, chosenApp: Application): Int =
    request(uri, chosenApp).header.status

  def request(uri: String, chosenApp: Application): Result = {
    val result = route(chosenApp, FakeRequest(GET, uri)).get
    await(result)
  }
}
