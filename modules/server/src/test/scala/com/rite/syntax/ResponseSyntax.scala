package com.rite.syntax

import sttp.client3.Response
import zio.json.JsonDecoder
import zio.json.DecoderOps

extension (response: Response[Either[String, String]]) {
  def tryParseTo[A: JsonDecoder]: Option[A] =
    response.body.toOption.flatMap(_.fromJson[A].toOption)
}
