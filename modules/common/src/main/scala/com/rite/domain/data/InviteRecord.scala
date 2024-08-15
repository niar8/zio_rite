package com.rite.domain.data

import zio.json.JsonCodec

final case class InviteRecord(
    id: Long,
    userName: String,
    companyId: Long,
    nInvites: Int,
    isActive: Boolean = false
) derives JsonCodec {}
