package com.rite

import com.rite.config.*
import com.rite.http.HttpApi
import com.rite.repositories.*
import com.rite.services.*
import sttp.tapir.*
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.ziohttp.*
import zio.*
import zio.http.{Server, ServerConfig}

import java.net.InetSocketAddress

object Application extends ZIOAppDefault {
  private val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    serverOptions = ZioHttpServerOptions.default[Any].appendInterceptor(CORSInterceptor.default)
    interpreter   = ZioHttpInterpreter(serverOptions)
    app <- ZIO.succeed(interpreter.toHttp(endpoints))
    _   <- Server.serve(app)
    _   <- Console.printLine("Server Started")
  } yield ()

  private val configuredServer: TaskLayer[Server] =
    Configs.makeConfigLayer[HttpConfig]("rite.http") >>>
      ZLayer {
        ZIO.serviceWith[HttpConfig] { config =>
          ServerConfig.default.copy(address = InetSocketAddress(config.port))
        }
      } >>> Server.live

  override def run: Task[Unit] = serverProgram.provide(
    configuredServer,
    // services
    UserServiceLive.layer,
    CompanyServiceLive.layer,
    ReviewServiceLive.configuredLayer,
    JWTServiceLive.configuredLayer,
    EmailServiceLive.configuredLayer,
    InviteServiceLive.configuredLayer,
    PaymentServiceLive.configuredLayer,
    OpenAIServiceLive.configuredLayer,
    // repos
    CompanyRepositoryLive.layer,
    ReviewRepositoryLive.layer,
    UserRepositoryLive.layer,
    RecoveryTokenRepositoryLive.configuredLayer,
    InviteRepositoryLive.layer,
    // other requirements
    Repository.dataLayer
  )
}
