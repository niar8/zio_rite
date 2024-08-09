package com.rite.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.StringAsIsCodec
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement
import sttp.client3.*
import org.scalajs.dom
import zio.*

/*
 *  1. Populate the panel with the right values
 *    a. expose some API that will retrieve the unique values for filtering
 *    b. fetch those values to populate the panel
 *  2. Update the filter panel when they interact with it
 *  3. When clicking "apply filters" we should retrieve just those companies
 *    a. make the backend search
 *    b. refetch companies when user clicks the filter
 */

object FilterPanel {
  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls    := "accordion accordion-flush",
      idAttr := "accordionFlushExample",
      div(
        cls := "accordion-item",
        h2(
          cls    := "accordion-header",
          idAttr := "flush-headingOne",
          button(
            cls                                         := "accordion-button",
            idAttr                                      := "accordion-search-filter",
            `type`                                      := "button",
            htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
            htmlAttr("data-bs-target", StringAsIsCodec) := "#flush-collapseOne",
            htmlAttr("aria-expanded", StringAsIsCodec)  := "true",
            htmlAttr("aria-controls", StringAsIsCodec)  := "flush-collapseOne",
            div(
              cls := "jvm-recent-companies-accordion-body-heading",
              h3(
                span("Search"),
                " Filters"
              )
            )
          )
        ),
        div(
          cls                                          := "accordion-collapse collapse show",
          idAttr                                       := "flush-collapseOne",
          htmlAttr("aria-labelledby", StringAsIsCodec) := "flush-headingOne",
          htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionFlushExample",
          div(
            cls := "accordion-body p-0",
            renderFilterOptions("Locations", List("London", "Paris")),
            renderFilterOptions("Countries", List("UK", "France")),
            renderFilterOptions("Industries", List("Banking", "Aviation")),
            renderFilterOptions("Tags", List("Scala", "ZIO", "Typelevel")),
            div(
              cls := "jvm-accordion-search-btn",
              button(
                cls    := "btn btn-primary",
                `type` := "button",
                "Apply Filters"
              )
            )
          )
        )
      )
    )

  def renderFilterOptions(
      groupName: String,
      options: List[String]
  ): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "accordion-item",
      h2(
        cls    := "accordion-header",
        idAttr := s"heading$groupName",
        button(
          cls                                         := "accordion-button collapsed",
          `type`                                      := "button",
          htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
          htmlAttr("data-bs-target", StringAsIsCodec) := s"#collapse$groupName",
          htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
          htmlAttr("aria-controls", StringAsIsCodec)  := s"collapse$groupName",
          groupName
        )
      ),
      div(
        cls                                          := "accordion-collapse collapse",
        idAttr                                       := s"collapse$groupName",
        htmlAttr("aria-labelledby", StringAsIsCodec) := "headingOne",
        htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionExample",
        div(
          cls := "accordion-body",
          div(
            cls := "mb-3",
            options.map { value =>
              div(
                cls := "form-check",
                label(
                  cls   := "form-check-label",
                  forId := s"filter-$groupName-$value",
                  value
                ),
                input(
                  cls    := "form-check-input",
                  `type` := "checkbox",
                  idAttr := s"filter-$groupName-$value"
                )
              )
            }
          )
        )
      )
    )
}
