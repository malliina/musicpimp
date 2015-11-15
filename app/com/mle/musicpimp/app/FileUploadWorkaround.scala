package com.mle.musicpimp.app

import play.api.BuiltInComponentsFromContext
import play.api.inject.{Injector, NewInstanceInjector, SimpleInjector}
import play.api.libs.Files.DefaultTemporaryFileCreator

/**
  * @see https://gist.github.com/Timshel/b39f7ca724b16a7eb01e
  */
trait FileUploadWorkaround extends BuiltInComponentsFromContext {
  lazy val defaultTemporaryFileCreator = new DefaultTemporaryFileCreator(applicationLifecycle)
  override lazy val injector: Injector = new SimpleInjector(NewInstanceInjector) + router + crypto + httpConfiguration + defaultTemporaryFileCreator
}
