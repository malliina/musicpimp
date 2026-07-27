package com.malliina.musicpimp.http

import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeError, toFlatMapOps, toFunctorOps}
import com.malliina.http.OkClient.MultiPartFile
import com.malliina.http.io.HttpClientF2
import com.malliina.http.{HttpResponse, OkClient}
import com.malliina.musicpimp.beam.BeamCommand
import com.malliina.musicpimp.library.MusicLibrary
import com.malliina.security.SSLUtils
import com.malliina.storage.StorageLong
import com.malliina.util.AppLogger
import com.malliina.values.ErrorMessage
import com.malliina.ws.HttpUtil
import okhttp3.MediaType
import play.api.http.HeaderNames

import java.nio.file.*
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.X509TrustManager
import scala.util.Try

object Rest:
  private val log = AppLogger(getClass)

  def canWriteNewFile(file: Path) =
    try
      val createdFile = Files.createFile(file)
      Files.delete(createdFile)
      true
    catch case _: Exception => false

  private object trustAllTrustManager extends X509TrustManager:
    override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = ()

    override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = ()

    override def getAcceptedIssuers: Array[X509Certificate] = Array.empty[X509Certificate]

  val okSslClient =
    OkClient.sslClient(SSLUtils.trustAllSslContext().getSocketFactory, trustAllTrustManager)
  val audioMpeg = MediaType.parse("audio/mpeg")

  def close(): Unit = ()

  def closeOk(client: OkClient) = Try:
    client.close()
    val inner = client.client
    inner.dispatcher().cancelAll()
    inner.dispatcher().executorService().shutdownNow()
    val terminated = inner.dispatcher().executorService().awaitTermination(5, TimeUnit.SECONDS)
    if !terminated then
      log.error("ExecutorService of HTTP client did not terminate in a timely manner.")

  /** Beams a track to a URI as specified in `cmd`.
    *
    * @param cmd
    *   beam details
    */
  def beam[F[_]: Async](
    cmd: BeamCommand,
    lib: MusicLibrary[F],
    client: HttpClientF2[F]
  ): F[Either[ErrorMessage, HttpResponse]] =
    val url = cmd.uri
    lib
      .findFile(cmd.track)
      .flatMap[Either[ErrorMessage, HttpResponse]]: maybeFile =>
        maybeFile
          .map: file =>
            val size = Files.size(file).bytes
            log.info(s"Beaming: $file of size: $size to: $url...")
            client
              .multiPart(
                url,
                Map(
                  HeaderNames.AUTHORIZATION -> HttpUtil
                    .authorizationValue(cmd.username.name, cmd.password.pass)
                ),
                files = Seq(MultiPartFile(audioMpeg, file))
              )
              .map[Either[ErrorMessage, HttpResponse]]: r =>
                if r.isSuccess then log.info(s"Beamed file: $file of size: $size to: $url")
                else log.error(s"Beam failed of file: $file of size: $size to: $url")
                Right(r)
          .getOrElse:
            Async[F].delay[Either[ErrorMessage, HttpResponse]](
              Left(ErrorMessage(s"Unable to find track with id: ${cmd.track}"))
            )
      .handleError:
        case e: Exception =>
          log.error("Beaming failed.", e)
          Left(ErrorMessage("Beaming failed."))
