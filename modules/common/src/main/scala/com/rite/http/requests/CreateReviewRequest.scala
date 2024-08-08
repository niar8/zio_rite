package com.rite.http.requests

import com.rite.domain.data.Review
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant

final case class CreateReviewRequest(
    companyId: Long,
    userId: Long,
    management: Int,
    culture: Int,
    salary: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String
) derives JsonCodec {
  def toReview(id: Long, userId: Long): Review =
    Review(
      id,
      companyId,
      userId,
      management,
      culture,
      salary,
      benefits,
      wouldRecommend,
      review,
      Instant.now(),
      Instant.now()
    )
}
