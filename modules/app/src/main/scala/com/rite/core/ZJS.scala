package com.rite.core

import com.raquo.laminar.api.L.{*, given}
import sttp.tapir.Endpoint
import zio.*

object ZJS {
  def useBackend[A]: ZIO.ServiceWithZIOPartiallyApplied[BackendClient] =
    ZIO.serviceWithZIO[BackendClient]

  extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A]) {
    def emitTo(eventBus: EventBus[A]): Fiber.Runtime[Throwable, A] =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.fork(
          zio
            .tap(value => ZIO.attempt(eventBus.emit(value)))
            .provide(BackendClientLive.configuredLayer)
        )
      }

    def toEventStream: EventStream[A] = {
      val eventBus = EventBus[A]()
      emitTo(eventBus)
      eventBus.events
    }

    def runJs: Fiber.Runtime[E, A] =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.fork(
          zio.provide(BackendClientLive.configuredLayer)
        )
      }
  }

  extension [I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any]) {
    def apply(payload: I): Task[O] =
      ZIO
        .serviceWithZIO[BackendClient](_.endpointRequestZIO(endpoint)(payload))
        .provide(BackendClientLive.configuredLayer)
  }

  extension [I, E <: Throwable, O](endpoint: Endpoint[String, I, E, O, Any]) {
    @annotation.targetName("applySecure")
    def apply(payload: I): Task[O] =
      ZIO
        .serviceWithZIO[BackendClient](_.secureEndpointRequestZIO(endpoint)(payload))
        .provide(BackendClientLive.configuredLayer)
  }
}
