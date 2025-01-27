package com.rite.domain.data

import zio.json.JsonCodec

final case class Company(
    id: Long,
    slug: String,
    name: String,
    url: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = List()
) derives JsonCodec

object Company {
  def makeSlug(name: String): String =
    name
      .replaceAll(" +", " ")
      .split(" ")
      .map(_.toLowerCase)
      .mkString("-")
}
