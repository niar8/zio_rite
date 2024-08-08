package com.rite.http.requests

import zio.json.JsonCodec

final case class ForgotPasswordRequest(
    email: String
) derives JsonCodec
