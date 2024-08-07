package com.rite

import com.raquo.airstream.ownership.OneTimeOwner
import com.raquo.airstream.timing.PeriodicStream
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.HTMLDivElement

import scala.util.Try

object Tutorial {
  val staticContent: ReactiveHtmlElement[HTMLDivElement] = div(
    styleAttr := "color:red", // <div style="color:red"/>
    p("This is an app"),
    p("Content from Laminar")
  )

  // EventStream - produces values of the same type
  val ticks: PeriodicStream[Int] = EventStream.periodic(intervalMs = 1000)
  // subscription - Airstream
  val subscription: Subscription = ticks.addObserver(new Observer[Int] {
    override def onError(err: Throwable): Unit    = ()
    override def onTry(nextValue: Try[Int]): Unit = ()
    override def onNext(nextValue: Int): Unit =
      dom.console.log(s"Ticks: $nextValue")
  })(new OneTimeOwner(() => ()))

  scala.scalajs.js.timers.setTimeout(interval = 10000)(subscription.kill())

  val timeUpdated: ReactiveHtmlElement[HTMLDivElement] = div(
    span("Time since loaded: "),
    child <-- ticks.map(n => s"$n seconds")
  )

  // EventBus - like EventStream, but you can push new elements to the stream
  val clickEvents: EventBus[Int] = EventBus[Int]()
  val clickUpdated: ReactiveHtmlElement[HTMLDivElement] = div(
    span("Clicks since loaded: "),
    child <-- clickEvents.events.scanLeft(0)(_ + _).map(n => s"$n clicks"),
    button(
      `type`    := "button",
      styleAttr := "display:block",
      "Add a click",
      onClick.map(_ => 1) --> clickEvents
    )
  )

  // Signal - similar to EventStreams, but it has a "current" value (state
  // and it can be inspected for the current state (if Laminar/Airstream knows that it has an owner)
  val countSignal: OwnedSignal[Int] =
    clickEvents.events.scanLeft(0)(_ + _).observe(new OneTimeOwner(() => ()))
  val queryEvents: EventBus[Unit] = EventBus[Unit]()
  val clicksQueued = div(
    span("Clicks since loaded: "),
    child <-- queryEvents.events.map(_ => countSignal.now()),
    button(
      `type`    := "button",
      styleAttr := "display:block",
      "Add a click",
      onClick.map(_ => 1) --> clickEvents
    ),
    button(
      `type`    := "button",
      styleAttr := "display:block",
      "Refresh the count",
      onClick.mapTo(()) --> queryEvents
    )
  )

  // Var - reactive variable
  val countVar = Var[Int](0)
  val clicksVar = div(
    span("Clicks since loaded: "),
    child <-- countVar.signal.map(_.toString),
    button(
      `type`    := "button",
      styleAttr := "display:block",
      "Add a click",
//      onClick --> countVar.updater((current, event) => current + 1)
//      onClick --> countVar.writer.contramap(event => countVar.now() + 1)
      onClick --> (_ => countVar.set(countVar.now() + 1))
    )

    /** no state | with state
      * ----------------------+------------ read EventStream | Signal
      * ----------------------+------------ write EventBus | Var
      */
  )
}
