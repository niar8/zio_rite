package com.rite.services

import zio.*
import zio.json.{DeriveJsonCodec, JsonCodec}
import com.rite.domain.data.Review
import com.rite.http.requests.CreateReviewRequest
import com.rite.repositories.ReviewRepository

import java.time.Instant

trait ReviewService {
  def create(request: CreateReviewRequest, userId: Long): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
}

class ReviewServiceLive private (repo: ReviewRepository) extends ReviewService {

  override def create(request: CreateReviewRequest, userId: Long): Task[Review] =
    repo.create(request.toReview(-1L, userId))

  override def getById(id: Long): Task[Option[Review]] =
    repo.getById(id)

  override def getByCompanyId(companyId: Long): Task[List[Review]] =
    repo.getByCompanyId(companyId)

  override def getByUserId(userId: Long): Task[List[Review]] =
    repo.getByUserId(userId)
}

object ReviewServiceLive {
  val layer: URLayer[ReviewRepository, ReviewService] = ZLayer {
    ZIO.serviceWith[ReviewRepository](new ReviewServiceLive(_))
  }
}
