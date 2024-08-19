package com.rite.services

import com.rite.config.{Configs, OpenAIConfig}
import com.rite.http.endpoints.OpenAIEndpoints
import com.rite.http.requests.CompletionRequest
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.model.Uri
import sttp.tapir.*
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

trait OpenAIService {
  def getCompletion(prompt: String): Task[Option[String]]
}

class OpenAIServiceLive private (
    backend: SttpBackend[Task, ZioStreams],
    interpreter: SttpClientInterpreter,
    config: OpenAIConfig
) extends OpenAIService
    with OpenAIEndpoints {
  override def getCompletion(prompt: String): Task[Option[String]] =
    secureEndpointRequestZIO(completionEndpoint)(CompletionRequest.single(prompt))
      .map(_.choices.map(_.message.content))
      .map(_.headOption)

  private def secureEndpointRequest[S, I, E, O](
      endpoint: Endpoint[S, I, E, O, Any]
  ): S => I => Request[Either[E, O], Any] =
    interpreter
      .toSecureRequestThrowDecodeFailures(endpoint, Uri.parse(config.baseUrl).toOption)

  private def secureEndpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I): Task[O] =
    backend
      .send(secureEndpointRequest(endpoint)(config.key)(payload))
      .map(_.body)
      .absolve
}

object OpenAIServiceLive {
  private type R = SttpClientInterpreter & SttpBackend[Task, ZioStreams]

  val layer: URLayer[OpenAIConfig & R, OpenAIService] = ZLayer {
    for {
      backend     <- ZIO.service[SttpBackend[Task, ZioStreams]]
      interpreter <- ZIO.service[SttpClientInterpreter]
      config      <- ZIO.service[OpenAIConfig]
    } yield new OpenAIServiceLive(backend, interpreter, config)
  }

  val configuredLayer: TaskLayer[OpenAIService] =
    HttpClientZioBackend.layer() >+>
      ZLayer.succeed(SttpClientInterpreter()) >+>
      Configs.makeConfigLayer[OpenAIConfig]("rite.openai") >>> layer
}

object OpenAIServiceDemo extends ZIOAppDefault {
  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    ZIO
      .serviceWithZIO[OpenAIService] {
        _.getCompletion("Please write a potential expansion of the acronym RTJVM, in one sentence.")
      }
      .flatMap(resp => Console.printLine(resp.toString))
      .provide(OpenAIServiceLive.configuredLayer)
}
