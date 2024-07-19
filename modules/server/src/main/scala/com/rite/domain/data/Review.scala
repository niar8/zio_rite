package com.rite.domain.data

import java.time.Instant
import zio.json.JsonCodec

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
