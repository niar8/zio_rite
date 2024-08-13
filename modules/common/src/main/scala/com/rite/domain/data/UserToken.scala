package com.rite.domain.data

import zio.json.JsonCodec

case class UserToken(
    id: Long,
    email: String,
    token: String,
    expiration: Long
) derives JsonCodec
