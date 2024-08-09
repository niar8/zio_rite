package com.rite.core

import com.raquo.laminar.api.L.{*, given}
import sttp.tapir.Endpoint
import zio.*

object ZJS {
  def useBackend[A]: ZIO.ServiceWithZIOPartiallyApplied[BackendClient] =
    ZIO.serviceWithZIO[BackendClient]

  extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A]) {
    def emitTo(eventBus: EventBus[A]) =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.fork(
          zio
            .tap(value => ZIO.attempt(eventBus.emit(value)))
            .provide(BackendClientLive.configuredLayer)
        )
      }
  }

  extension [I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any]) {
    def apply(payload: I): Task[O] =
      ZIO
        .serviceWithZIO[BackendClient](_.endpointRequestZIO(endpoint)(payload))
        .provide(BackendClientLive.configuredLayer)
  }
}
