package com.rite.http.controllers

import com.rite.domain.data.{Company, UserId}
import com.rite.http.endpoints.CompanyEndpoints
import com.rite.services.{CompanyService, JWTService}
import sttp.tapir.server.ServerEndpoint
import zio.*

import collection.mutable

class CompanyController private (
    companyService: CompanyService,
    jwtService: JWTService
) extends BaseController
    with CompanyEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint
      .serverSecurityLogic[UserId, Task](jwtService.verifyToken(_).either)
      .serverLogic(_ => req => companyService.create(req).either)

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic { id =>
      ZIO.succeed(id.toLongOption).flatMap {
        _.fold(companyService.getBySlug(id))(companyService.getById).either
      }
    }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogic(_ => companyService.getAll.either)

  val getAllFilters: ServerEndpoint[Any, Task] =
    getAllFiltersEndpoint.serverLogic { _ =>
      companyService.getAllFilters.either
    }

  // 'getAllFilters' must be processed before 'getById' to avoid path shadowing
  override val routes: List[ServerEndpoint[Any, Task]] =
    List(create, getAllFilters, getById, getAll)
}

object CompanyController {
  val makeZIO: URIO[JWTService & CompanyService, CompanyController] =
    for {
      companyService <- ZIO.service[CompanyService]
      jwtService     <- ZIO.service[JWTService]
    } yield new CompanyController(companyService, jwtService)
}
