package com.rite.http.requests

import zio.json.JsonCodec

final case class RegisterUserAccountRequest(
    email: String,
    password: String
) derives JsonCodec
