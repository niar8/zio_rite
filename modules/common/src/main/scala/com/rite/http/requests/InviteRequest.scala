package com.rite.http.requests

import zio.json.JsonCodec

final case class InviteRequest(
    companyId: Long,
    emails: List[String]
) derives JsonCodec
