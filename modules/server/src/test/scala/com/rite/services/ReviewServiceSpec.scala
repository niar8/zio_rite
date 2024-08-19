package com.rite.services

import com.rite.config.SummaryConfig
import com.rite.domain.data.{Review, ReviewSummary}
import com.rite.http.requests.CreateReviewRequest
import com.rite.repositories.ReviewRepository
import zio.*
import zio.test.*

import java.time.Instant

object ReviewServiceSpec extends ZIOSpecDefault {
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

  private val badReview = Review(
    id = 2L,
    companyId = 1L,
    userId = 1L,
    management = 1,
    culture = 1,
    salary = 1,
    benefits = 1,
    wouldRecommend = 1,
    review = "BAD BAD",
    created = Instant.now(),
    updated = Instant.now()
  )

  private val reviewRepoStubLayer: ULayer[ReviewRepository] = ZLayer.succeed {
    new ReviewRepository {
      override def create(review: Review): Task[Review] =
        ZIO.succeed(goodReview)
      override def getById(id: Long): Task[Option[Review]] =
        ZIO.succeed {
          id match {
            case 1 => Some(goodReview)
            case 2 => Some(badReview)
            case _ => None
          }
        }
      override def getAll: Task[List[Review]] =
        ZIO.succeed(List(goodReview))
      override def getByCompanyId(companyId: Long): Task[List[Review]] =
        ZIO.succeed { if (companyId == 1) List(goodReview, badReview) else Nil }
      override def getByUserId(userId: Long): Task[List[Review]] =
        ZIO.succeed { if (userId == 1) List(goodReview, badReview) else Nil }
      override def update(id: Long, op: Review => Review): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"id $id not found ")).map(op)
      override def delete(id: Long): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"id $id not found "))
      override def getSummary(companyId: Long): Task[Option[ReviewSummary]] =
        ZIO.none
      override def insertSummary(companyId: Long, summary: String): Task[ReviewSummary] =
        ZIO.succeed(ReviewSummary(companyId, contents = summary, created = Instant.now))
    }
  }

  private val openaiServiceStubLayer: ULayer[OpenAIService] = ZLayer.succeed {
    new OpenAIService {
      override def getCompletion(prompt: String): Task[Option[String]] =
        ZIO.none
    }
  }

  private val summaryConfigLayer = ZLayer.succeed(SummaryConfig(minReviews = 3, nSelected = 20))

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewServiceSpec")(
      test("create") {
        for {
          service <- ZIO.service[ReviewService]
          review <- service.create(
            CreateReviewRequest(
              companyId = goodReview.companyId,
              userId = goodReview.userId,
              management = goodReview.management,
              culture = goodReview.culture,
              salary = goodReview.salary,
              benefits = goodReview.benefits,
              wouldRecommend = goodReview.wouldRecommend,
              review = goodReview.review
            ),
            userId = 1L
          )
        } yield assertTrue(
          review.companyId == goodReview.companyId &&
            review.userId == goodReview.userId &&
            review.management == goodReview.management &&
            review.culture == goodReview.culture &&
            review.salary == goodReview.salary &&
            review.benefits == goodReview.benefits &&
            review.wouldRecommend == goodReview.wouldRecommend &&
            review.review == goodReview.review
        )
      },
      test("get by id") {
        for {
          service        <- ZIO.service[ReviewService]
          review         <- service.getById(1L)
          reviewNotFound <- service.getById(999L)
        } yield assertTrue(
          review.contains(goodReview) &&
            reviewNotFound.isEmpty
        )
      },
      test("get by companyId") {
        for {
          service        <- ZIO.service[ReviewService]
          review         <- service.getByCompanyId(1L)
          reviewNotFound <- service.getByCompanyId(999L)
        } yield assertTrue(
          review.toSet == Set(goodReview, badReview) &&
            reviewNotFound.isEmpty
        )
      },
      test("get by userId") {
        for {
          service        <- ZIO.service[ReviewService]
          review         <- service.getByUserId(1L)
          reviewNotFound <- service.getByUserId(999L)
        } yield assertTrue(
          review.toSet == Set(goodReview, badReview) &&
            reviewNotFound.isEmpty
        )
      }
    ).provide(
      ReviewServiceLive.layer,
      reviewRepoStubLayer,
      openaiServiceStubLayer,
      summaryConfigLayer
    )
}
