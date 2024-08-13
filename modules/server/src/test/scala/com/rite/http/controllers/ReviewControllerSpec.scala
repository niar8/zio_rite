package com.rite.http.controllers

import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.client3.*
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import zio.*
import zio.test.*
import zio.json.*

import java.time.Instant
import com.rite.domain.data.{Review, User, UserId, UserToken}
import com.rite.http.requests.CreateReviewRequest
import com.rite.services.{JWTService, ReviewService}
import com.rite.syntax.*

object ReviewControllerSpec extends ZIOSpecDefault {

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val goodReview = Review(
    id = 1L,
    companyId = 1L,
    userId = 1L,
    management = 5,
    culture = 5,
    salary = 5,
    benefits = 5,
    wouldRecommend = 10,
    review = "all good",
    created = Instant.now(),
    updated = Instant.now()
  )

  private val createReviewRequest = CreateReviewRequest(
    companyId = 1L,
    userId = 1L,
    management = 5,
    culture = 5,
    salary = 5,
    benefits = 5,
    wouldRecommend = 10,
    review = "all good"
  )

  private def backendStubZIO(
      endpointFun: ReviewController => ServerEndpoint[Any, Task]
  ): URIO[JWTService & ReviewService, SttpBackend[Task, Any]] =
    for {
      controller <- ReviewController.makeZIO
      backendStub <- ZIO.succeed {
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointRunLogic(endpointFun(controller))
          .backend()
      }
    } yield backendStub

  private val jwtServiceStub = new JWTService {
    override def createToken(user: User): Task[UserToken] =
      ZIO.succeed(UserToken(user.id, user.email, "BIG ACCESS", Long.MaxValue))
    override def verifyToken(token: String): Task[UserId] =
      ZIO.succeed(UserId(1L, "rite@rite.com"))
  }

  private val reviewServiceStub = new ReviewService {
    override def create(request: CreateReviewRequest, userId: Long): Task[Review] =
      ZIO.succeed(goodReview)
    override def getById(id: Long): Task[Option[Review]] =
      ZIO.succeed { if (id == 1L) Some(goodReview) else None }
    override def getByCompanyId(companyId: Long): Task[List[Review]] =
      ZIO.succeed { if (companyId == 1L) List(goodReview) else Nil }
    override def getByUserId(userId: Long): Task[List[Review]] =
      ZIO.succeed { if (userId == 1L) List(goodReview) else Nil }
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewControllerSpec")(
      test("post review") {
        for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"reviews")
            .body(createReviewRequest.toJson)
            .header("Authorization", "Bearer BIG ACCESS")
            .send(backendStub)
        } yield assertTrue(
          response.tryParseTo[Review].contains(goodReview)
        )
      },
      test("get by id") {
        for {
          backendStub      <- backendStubZIO(_.getById)
          response         <- basicRequest.get(uri"reviews/1").send(backendStub)
          responseNotFound <- basicRequest.get(uri"reviews/999").send(backendStub)
        } yield assertTrue(
          response.tryParseTo[Review].contains(goodReview) &&
            responseNotFound.tryParseTo[Review].isEmpty
        )
      },
      test("get by company id") {
        for {
          backendStub      <- backendStubZIO(_.getByCompanyId)
          response         <- basicRequest.get(uri"reviews/company/1").send(backendStub)
          responseNotFound <- basicRequest.get(uri"reviews/company/999").send(backendStub)
        } yield assertTrue(
          response.tryParseTo[List[Review]].contains(List(goodReview)) &&
            responseNotFound.tryParseTo[List[Review]].contains(List())
        )
      }
    ).provide(
      ZLayer.succeed(reviewServiceStub),
      ZLayer.succeed(jwtServiceStub)
    )
}
