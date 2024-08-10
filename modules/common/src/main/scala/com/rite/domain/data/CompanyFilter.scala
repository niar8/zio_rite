package com.rite.domain.data

import zio.json.JsonCodec

final case class CompanyFilter(
    locations: List[String] = Nil,
    countries: List[String] = Nil,
    industries: List[String] = Nil,
    tags: List[String] = Nil
) derives JsonCodec {
  val isEmpty: Boolean =
    locations.isEmpty && countries.isEmpty && industries.isEmpty && tags.isEmpty
}

object CompanyFilter {
  val empty: CompanyFilter = CompanyFilter()
}
