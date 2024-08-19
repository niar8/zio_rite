package com.rite.http.responses

import zio.json.JsonCodec

final case class CompletionMessage(
    content: String,
    role: String
) derives JsonCodec

final case class Completion(
    index: Int,
    message: CompletionMessage
) derives JsonCodec

final case class CompletionResponse(
    id: String,
    created: Long,
    choices: List[Completion]
) derives JsonCodec
