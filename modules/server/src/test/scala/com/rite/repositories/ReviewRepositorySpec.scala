package com.rite.repositories

import com.rite.domain.data.Review
import com.rite.syntax.*
import zio.*
import zio.test.*

import java.time.Instant

object ReviewRepositorySpec extends ZIOSpecDefault with RepositorySpec {
  private val goodReview: Review = Review(
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

  private val badReview: Review = Review(
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

  override val initScript: String = "sql/reviews.sql"

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewRepositorySpec")(
      test("create a review") {
        val program = for {
          repo   <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
        } yield review

        program.assert { review =>
          review.management == goodReview.management &&
          review.culture == goodReview.culture &&
          review.salary == goodReview.salary &&
          review.benefits == goodReview.benefits &&
          review.wouldRecommend == goodReview.wouldRecommend &&
          review.review == goodReview.review
        }
      },
      test("get a review by ids (id, companyId, userId)") {
        for {
          repo               <- ZIO.service[ReviewRepository]
          review             <- repo.create(goodReview)
          fetchedById        <- repo.getById(review.id)
          fetchedByCompanyId <- repo.getByCompanyId(review.companyId)
          fetchedByUserId    <- repo.getByUserId(review.userId)
        } yield assertTrue(
          fetchedById.contains(review) &&
            fetchedByCompanyId.contains(review) &&
            fetchedByUserId.contains(review)
        )
      },
      test("get reviews by company id or by user id") {
        for {
          repo                <- ZIO.service[ReviewRepository]
          review1             <- repo.create(goodReview)
          review2             <- repo.create(badReview)
          reviewsAboutCompany <- repo.getByCompanyId(1L)
          reviewsByUser       <- repo.getByUserId(1L)
        } yield assertTrue(
          reviewsAboutCompany.toSet == Set(review1, review2) &&
            reviewsByUser.toSet == Set(review1, review2)
        )
      },
      test("edit a review") {
        for {
          repo    <- ZIO.service[ReviewRepository]
          review  <- repo.create(goodReview)
          updated <- repo.update(review.id, _.copy(review = "not too bad"))
        } yield assertTrue(
          review.id == updated.id &&
            review.companyId == updated.companyId &&
            review.userId == updated.userId &&
            review.management == updated.management &&
            review.culture == updated.culture &&
            review.salary == updated.salary &&
            review.benefits == updated.benefits &&
            review.wouldRecommend == updated.wouldRecommend &&
            review.review != updated.review &&
            review.created == updated.created &&
            review.updated != updated.updated
        )
      },
      test("delete a review") {
        for {
          repo        <- ZIO.service[ReviewRepository]
          review      <- repo.create(goodReview)
          _           <- repo.delete(review.id)
          maybeReview <- repo.getById(review.id)
        } yield assertTrue(maybeReview.isEmpty)
      }
    ).provide(
      ReviewRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
}
