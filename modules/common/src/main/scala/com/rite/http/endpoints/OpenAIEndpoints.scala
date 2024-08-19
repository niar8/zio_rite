package com.rite.http.endpoints

import com.rite.domain.errors.HttpError
import com.rite.http.requests.CompletionRequest
import com.rite.http.responses.CompletionResponse
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import zio.*

trait OpenAIEndpoints extends Endpoints {
  val completionEndpoint = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)
    .securityIn(auth.bearer[String]())
    .in("v1" / "chat" / "completions")
    .post
    .in(jsonBody[CompletionRequest])
    .out(jsonBody[CompletionResponse])
}
