package com.rite.config

final case class EmailServiceConfig(
    host: String,
    port: Int,
    user: String,
    pass: String,
    baseUrl: String
)
