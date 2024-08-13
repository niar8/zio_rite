package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.common.*
import com.rite.components.*
import com.rite.core.ZJS.*
import com.rite.domain.data.*
import org.scalajs.dom
import org.scalajs.dom.{HTMLDivElement, HTMLElement}

object CompaniesPage {

  // components
  private val filterPanel = new FilterPanel

  def apply(): ReactiveHtmlElement[HTMLElement] =
    sectionTag(
      onMountCallback { _ =>
        useBackend(_.company.getAllEndpoint(payload = ())).emitTo(firstBatch)
      },
      cls := "section-1",
      div(
        cls := "container company-list-hero",
        h1(
          cls := "company-list-title",
          "Rock the JVM Companies Board"
        )
      ),
      div(
        cls := "container",
        div(
          cls := "row jvm-recent-companies-body",
          div(
            cls := "col-lg-4",
            filterPanel()
          ),
          div(
            cls := "col-lg-8",
            children <-- companyEvents.map(_.map(renderCompany))
          )
        )
      )
    )

  private val firstBatch: EventBus[List[Company]] = EventBus[List[Company]]()

  private val companyEvents: EventStream[List[Company]] =
    firstBatch.events.mergeWith {
      filterPanel.triggerFilters.flatMap { newFilter =>
        useBackend(_.company.searchByFilterEndpoint(payload = newFilter)).toEventStream
      }
    }

  private def renderCompany(company: Company) =
    div(
      cls := "jvm-recent-companies-cards",
      div(
        cls := "jvm-recent-companies-card-img",
        CompanyComponents.renderCompanyPicture(company)
      ),
      div(
        cls := "jvm-recent-companies-card-contents",
        h5(
          Anchors.renderNavLink(
            company.name,
            s"/company/${company.id}",
            "company-title-link"
          )
        ),
        CompanyComponents.renderOverview(company)
      ),
      renderAction(company)
    )

  private def renderAction(company: Company) =
    div(
      cls := "jvm-recent-companies-card-btn-apply",
      a(
        href   := company.url,
        target := "blank",
        button(
          `type` := "button",
          cls    := "btn btn-danger rock-action-btn",
          "Website"
        )
      )
    )
}
