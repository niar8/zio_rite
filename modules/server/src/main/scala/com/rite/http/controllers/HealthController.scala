package com.rite.http.controllers

import sttp.tapir.*
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import zio.*

import com.rite.domain.errors.HttpError
import com.rite.http.endpoints.HealthEndpoints

class HealthController private extends BaseController with HealthEndpoints {
  val health: ServerEndpoint[Any, Task] =
    healthEndpoint
      .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  val errorRoute: ServerEndpoint[Any, Task] =
    errorEndpoint
      .serverLogic[Task](_ => ZIO.fail(new RuntimeException("Boom!")).either)

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(health, errorRoute)
}

object HealthController {
  val makeZIO: UIO[HealthController] = ZIO.succeed(new HealthController)
}
