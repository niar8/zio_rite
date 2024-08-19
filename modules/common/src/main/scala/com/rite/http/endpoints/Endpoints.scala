package com.rite.http.endpoints

import com.rite.domain.errors.HttpError
import sttp.tapir.*

trait Endpoints {
  val baseEndpoint = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)
    .prependIn("api")

  val secureBaseEndpoint =
    baseEndpoint.securityIn(auth.bearer[String]())
}
