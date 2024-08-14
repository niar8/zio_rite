package com.rite.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.core.ZJS.*
import com.rite.domain.data.Review
import com.rite.http.requests.CreateReviewRequest
import org.scalajs.dom.HTMLDivElement
import zio.*

class AddReviewCard(companyId: Long, onDisable: () => Unit, triggerBus: EventBus[Unit]) {
  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        div(
          cls := "company-description add-review",
          div(
            div(
              cls := "add-review-scores",
              renderDropdown("Would recommend", (r, v) => r.copy(wouldRecommend = v)),
              renderDropdown("Management", (r, v) => r.copy(management = v)),
              renderDropdown("Culture", (r, v) => r.copy(culture = v)),
              renderDropdown("Salary", (r, v) => r.copy(salary = v)),
              renderDropdown("Benefits", (r, v) => r.copy(benefits = v))
            ),
            div(
              cls := "add-review-text",
              label(forId := "add-review-text", "Your review - supports Markdown"),
              textArea(
                idAttr      := "add-review-text",
                cls         := "add-review-text-input",
                placeholder := "Write your review here",
                onInput.mapToValue --> stateVar.updater { (state: State, newReview: String) =>
                  state.copy(review = state.review.copy(review = newReview))
                }
              )
            ),
            button(
              `type` := "button",
              cls    := "btn btn-warning rock-action-btn",
              "Post review",
              onClick.preventDefault.mapTo(stateVar.now()) --> submitter
            ),
            a(
              cls  := "add-review-cancel",
              href := "#",
              "Cancel",
              onClick --> (_ => onDisable())
            ),
            children <-- stateVar.signal
              .map(s => s.upstreamError.filter(_ => s.showErrors))
              .map(maybeRenderError)
              .map(_.toList)
          )
        )
      )
    )

  private final case class State(
      review: Review = Review.empty(companyId),
      showErrors: Boolean = false,
      upstreamError: Option[String] = None
  )

  private val stateVar: Var[State] = Var(State())

  private val submitter: Observer[State] = Observer[State] { state =>
    if (state.upstreamError.nonEmpty) {
      stateVar.update(_.copy(showErrors = true))
    } else
      useBackend(
        _.review.createEndpoint(
          payload = CreateReviewRequest.fromReview(state.review)
        )
      )
        .map { resp => onDisable() }
        .tapError { e =>
          ZIO.succeed {
            stateVar.update(_.copy(showErrors = true, upstreamError = Some(e.getMessage)))
          }
        }
        .emitTo(triggerBus)
  }

  private def renderDropdown(
      name: String,
      updateFn: (Review, Int) => Review
  ): ReactiveHtmlElement[HTMLDivElement] = {
    val selectorId = name.split(" ").map(_.toLowerCase).mkString("-")
    div(
      cls := "add-review-score",
      label(forId := selectorId, s"$name:"),
      select(
        idAttr := selectorId,
        (1 to 5).reverse.map { v => option(v.toString) },
        onInput.mapToValue --> stateVar.updater { (s: State, value: String) =>
          s.copy(review = updateFn(s.review, value.toInt))
        }
      )
    )
  }

  private def maybeRenderError(maybeError: Option[String]) =
    maybeError.map { div(cls := "page-status-error", _) }
}
