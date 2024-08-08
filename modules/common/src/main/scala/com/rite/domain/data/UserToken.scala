package com.rite.domain.data

import zio.json.JsonCodec

case class UserToken(
    email: String,
    token: String,
    expiration: Long
) derives JsonCodec
