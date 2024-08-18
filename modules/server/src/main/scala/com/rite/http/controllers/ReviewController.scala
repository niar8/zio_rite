package com.rite.http.controllers

import com.rite.domain.data.UserId
import sttp.tapir.server.ServerEndpoint
import zio.*
import com.rite.http.endpoints.ReviewEndpoints
import com.rite.services.{JWTService, ReviewService}

class ReviewController private (
    reviewService: ReviewService,
    jwtService: JWTService
) extends BaseController
    with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint
      .serverSecurityLogic[UserId, Task](jwtService.verifyToken(_).either)
      .serverLogic { userId => req =>
        reviewService.create(req, userId.id).either
      }

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic(reviewService.getById(_).either)

  val getByCompanyId: ServerEndpoint[Any, Task] =
    getByCompanyIdEndpoint.serverLogic(reviewService.getByCompanyId(_).either)

  val getByUserId: ServerEndpoint[Any, Task] =
    getByUserIdEndpoint.serverLogic(reviewService.getByUserId(_).either)

  val getSummary: ServerEndpoint[Any, Task] =
    getSummaryEndpoint.serverLogic(reviewService.getSummary(_).either)

  val makeSummary: ServerEndpoint[Any, Task] =
    makeSummaryEndpoint.serverLogic(reviewService.makeSummary(_).either)

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(getSummary, makeSummary, create, getById, getByCompanyId, getByUserId)
}

object ReviewController {
  val makeZIO: URIO[JWTService & ReviewService, ReviewController] =
    for {
      reviewService <- ZIO.service[ReviewService]
      jwtService    <- ZIO.service[JWTService]
    } yield new ReviewController(reviewService, jwtService)
}
