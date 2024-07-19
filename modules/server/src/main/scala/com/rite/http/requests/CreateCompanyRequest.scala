package com.rite.http.requests

import com.rite.domain.data.Company
import zio.json.{DeriveJsonCodec, JsonCodec}

final case class CreateCompanyRequest(
    name: String,
    url: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: Option[List[String]] = None
) derives JsonCodec {
  def toCompany(id: Long): Company =
    Company(
      id,
      Company.makeSlug(name),
      name,
      url,
      location,
      country,
      industry,
      image,
      tags.getOrElse(Nil)
    )
}
