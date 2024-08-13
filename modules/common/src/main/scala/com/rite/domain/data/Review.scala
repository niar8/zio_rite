package com.rite.domain.data

import zio.json.JsonCodec

import java.time.Instant

case class Review(
    id: Long, // PK
    companyId: Long,
    userId: Long,    // FK
    management: Int, // 1-5
    culture: Int,
    salary: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String,
    created: Instant,
    updated: Instant
) derives JsonCodec

object Review {
  def empty(companyId: Long): Review = Review(
    id = -1L,
    companyId,
    userId = -1L,
    management = 5,
    culture = 5,
    salary = 5,
    benefits = 5,
    wouldRecommend = 5,
    review = "",
    created = Instant.now(),
    updated = Instant.now()
  )
}
