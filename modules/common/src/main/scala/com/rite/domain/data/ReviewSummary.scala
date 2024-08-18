package com.rite.domain.data

import zio.json.JsonCodec

import java.time.Instant

final case class ReviewSummary(
    companyId: Long,
    contents: String,
    created: Instant
) derives JsonCodec
