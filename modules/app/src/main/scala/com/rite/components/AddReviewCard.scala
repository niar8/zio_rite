package com.rite.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

class AddReviewCard(companyId: Long, onCancel: () => Unit) {
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
              label(forId := "would-recommend-selector", "Would recommend:"),
              select(
                idAttr := "would-recommend-selector",
                (1 to 5).reverse.map { v =>
                  option(v.toString)
                // TODO set state here
                }
              )
              // TODO the same for all score fields
            ),
            div(
              cls := "add-review-text",
              label(forId := "add-review-text", "Your review - supports Markdown"),
              textArea(
                idAttr      := "add-review-text",
                cls         := "add-review-text-input",
                placeholder := "Write your review here"
                // TODO set state here
              )
            ),
            button(
              `type` := "button",
              cls    := "btn btn-warning rock-action-btn",
              "Post review"
              // TODO post the review on this button
            ),
            a(
              cls  := "add-review-cancel",
              href := "#",
              "Cancel",
              onClick --> (_ => onCancel())
            )
            // TODO show potential errors here
          )
        )
      )
    )
}
