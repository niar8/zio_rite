package com.rite.config

final case class StripeConfig(
    key: String,
    secret: String,
    price: String,
    successUrl: String,
    cancelUrl: String
)
