package com.rite.config

final case class StripeConfig(
    key: String,
    price: String,
    successUrl: String,
    cancelUrl: String
)
