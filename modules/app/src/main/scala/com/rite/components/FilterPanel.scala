package com.rite.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.StringAsIsCodec
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.core.ZJS.*
import com.rite.domain.data.CompanyFilter
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
  private final case class CheckValueEvent(groupName: String, value: String, checked: Boolean)

  private val GROUP_LOCATIONS  = "Locations"
  private val GROUP_COUNTRIES  = "Countries"
  private val GROUP_INDUSTRIES = "Industries"
  private val GROUP_TAGS       = "Tags"

  private val possibleFilter = Var[CompanyFilter](CompanyFilter.empty)
  private val checkEvents    = EventBus[CheckValueEvent]()

  // clicks on the "apply filters" button
  private val clicks = EventBus[Unit]()

  // emit either true or false depending to show "apply" or not
  private val dirty = clicks.events.mapTo(false).mergeWith(clicks.events.mapTo(true))

  private val state: Signal[CompanyFilter] =
    checkEvents.events
      .scanLeft(Map.empty[String, Set[String]]) { (currentMap, event) =>
        event match {
          case CheckValueEvent(groupName, value, checked) =>
            val currentSet = currentMap.getOrElse(groupName, Set.empty)
            if (checked)
              currentMap + (groupName -> (currentSet + value))
            else
              currentMap + (groupName -> (currentSet - value))
        }
      }
      .map { checkMap =>
        CompanyFilter(
          locations = checkMap.getOrElse(GROUP_LOCATIONS, Set.empty).toList,
          countries = checkMap.getOrElse(GROUP_COUNTRIES, Set.empty).toList,
          industries = checkMap.getOrElse(GROUP_INDUSTRIES, Set.empty).toList,
          tags = checkMap.getOrElse(GROUP_TAGS, Set.empty).toList
        )
      }

  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    div(
      onMountCallback { _ =>
        useBackend(_.company.getAllFiltersEndpoint(payload = ()))
          .map(possibleFilter.set)
          .runJs
      },
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
            renderFilterOptions(GROUP_LOCATIONS, _.locations),
            renderFilterOptions(GROUP_COUNTRIES, _.countries),
            renderFilterOptions(GROUP_INDUSTRIES, _.industries),
            renderFilterOptions(GROUP_TAGS, _.tags),
            renderApplyFiltersButton()
          )
        )
      )
    )

  private def renderFilterOptions(
      groupName: String,
      optionsFn: CompanyFilter => List[String]
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
            children <-- possibleFilter.signal.map {
              optionsFn(_).map(renderCheckbox(groupName, _))
            }
          )
        )
      )
    )

  private def renderCheckbox(groupName: String, value: String) =
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
        idAttr := s"filter-$groupName-$value",
        onChange.mapToChecked.map(CheckValueEvent(groupName, value, _)) --> checkEvents
      )
    )

  private def renderApplyFiltersButton() =
    div(
      cls := "jvm-accordion-search-btn",
      button(
        disabled <-- dirty.toSignal(false).map(v => !v),
        onClick.mapTo(()) --> clicks,
        cls    := "btn btn-primary",
        `type` := "button",
        "Apply Filters"
      )
    )
}
