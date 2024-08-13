package com.rite.core

import com.rite.configs.BackendClientConfig
import com.rite.http.endpoints.{CompanyEndpoints, ReviewEndpoints, UserEndpoints}
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.impl.zio.FetchZioBackend
import sttp.client3.{Request, SttpBackend, UriContext}
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

final case class RestrictedEndpointException(
    message: String
) extends RuntimeException(message)

trait BackendClient {
  val company: CompanyEndpoints
  val user: UserEndpoints
  val review: ReviewEndpoints

  def endpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(payload: I): Task[O]

  def secureEndpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I): Task[O]
}

class BackendClientLive(
    backend: SttpBackend[Task, ZioStreams & WebSockets],
    interpreter: SttpClientInterpreter,
    config: BackendClientConfig
) extends BackendClient {
  override val company: CompanyEndpoints = new CompanyEndpoints {}
  override val user: UserEndpoints       = new UserEndpoints {}
  override val review: ReviewEndpoints   = new ReviewEndpoints {}

  override def endpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(payload: I): Task[O] =
    backend.send(endpointRequest(endpoint)(payload)).map(_.body).absolve

  override def secureEndpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I): Task[O] =
    for {
      token    <- tokenOrFail
      response <- backend.send(secureEndpointRequest(endpoint)(token)(payload)).map(_.body).absolve
    } yield response

  private def endpointRequest[I, E, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  ): I => Request[Either[E, O], Any] =
    interpreter.toRequestThrowDecodeFailures(endpoint, config.uri)

  private def secureEndpointRequest[S, I, E, O](
      endpoint: Endpoint[S, I, E, O, Any]
  ): S => I => Request[Either[E, O], Any] =
    interpreter.toSecureRequestThrowDecodeFailures(endpoint, config.uri)

  private def tokenOrFail: IO[RestrictedEndpointException, String] =
    ZIO
      .fromOption(Session.getUserState)
      .mapBoth(_ => RestrictedEndpointException("You need to log in"), _.token)
}

object BackendClientLive {
  private type Backend = SttpBackend[Task, ZioStreams & WebSockets]
  private type R       = BackendClientConfig & SttpClientInterpreter & Backend

  val layer: URLayer[R, BackendClient] = ZLayer {
    for {
      backend     <- ZIO.service[SttpBackend[Task, ZioStreams & WebSockets]]
      interpreter <- ZIO.service[SttpClientInterpreter]
      config      <- ZIO.service[BackendClientConfig]
    } yield new BackendClientLive(backend, interpreter, config)
  }

  val configuredLayer: ULayer[BackendClient] = {
    val backend     = FetchZioBackend()
    val interpreter = SttpClientInterpreter()
    val config      = BackendClientConfig(Some(uri"http://localhost:8080"))
    ZLayer.succeed(backend) ++
      ZLayer.succeed(interpreter) ++
      ZLayer.succeed(config) >>> layer
  }
}
