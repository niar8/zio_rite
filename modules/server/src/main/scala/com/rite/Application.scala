package com.rite

import com.rite.config.{Configs, JWTConfig}
import com.rite.http.HttpApi
import com.rite.http.controllers.*
import com.rite.services.*
import com.rite.repositories.*
import sttp.tapir.*
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.ziohttp.*
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  private val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    serverOptions = ZioHttpServerOptions.default[Any].appendInterceptor(CORSInterceptor.default)
    interpreter <- ZIO.succeed(ZioHttpInterpreter(serverOptions))
    app         <- ZIO.succeed(interpreter.toHttp(endpoints))
    _           <- Server.serve(app)
    _           <- Console.printLine("Server Started")
  } yield ()

  override def run: Task[Unit] = serverProgram.provide(
    Server.default,
    // configs
    // services
    CompanyServiceLive.layer,
    ReviewServiceLive.layer,
    UserServiceLive.layer,
    JWTServiceLive.configuredLayer,
    EmailServiceLive.configuredLayer,
    InviteServiceLive.layer,
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
