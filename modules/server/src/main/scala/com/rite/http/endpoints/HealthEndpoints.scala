package com.rite.http.endpoints

import sttp.tapir.*

trait HealthEndpoints extends Endpoints {
  // GET /health
  val healthEndpoint = baseEndpoint
    .tag("health")
    .name("health")
    .description("health check")
    .get
    .in("health")
    .out(plainBody[String])

  // GET /health/error
  val errorEndpoint = baseEndpoint
    .tag("health")
    .name("error health")
    .description("health check - should fail")
    .get
    .in("health" / "error")
    .out(plainBody[String])
}
