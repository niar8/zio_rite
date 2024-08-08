package com.rite.pages

import scala.scalajs.js.annotation.*
import scala.scalajs.js

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontroute.*
import org.scalajs.dom
import org.scalajs.dom.{HTMLDivElement, HTMLElement}
import sttp.client3.UriContext
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

import com.rite.common.*
import com.rite.components.*
import com.rite.domain.data.*
import com.rite.http.endpoints.CompanyEndpoints

object CompaniesPage {
  def apply(): ReactiveHtmlElement[HTMLElement] =
    sectionTag(
      onMountCallback(_ => performBackendCall()),
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
            div("TODO filter panel here")
          ),
          div(
            cls := "col-lg-8",
            children <-- companiesBus.events.map(_.map(renderCompany))
          )
        )
      )
    )

  private val dummyCompany = Company(
    1L,
    "simple-company",
    "Simple company",
    "http://dummy.com",
    Some("Anywhere"),
    Some("On Mars"),
    Some("space travel"),
    None,
    List("space", "scala")
  )

  private val companiesBus: EventBus[List[Company]] = EventBus[List[Company]]()

  private def performBackendCall(): Unit = {
    val companyEndpoints                   = new CompanyEndpoints {}
    val theEndpoint                        = companyEndpoints.getAllEndpoint
    val backend                            = FetchZioBackend()
    val interpreter: SttpClientInterpreter = SttpClientInterpreter()
    val request = interpreter
      .toRequestThrowDecodeFailures(
        theEndpoint,
        Some(uri"http://localhost:8080")
      )
      .apply(())
    val companiesZIO: Task[List[Company]] = backend.send(request).map(_.body).absolve
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.fork(
        companiesZIO.tap(list => ZIO.attempt(companiesBus.emit(list)))
      )
    }
  }

  private def renderCompany(company: Company) =
    div(
      cls := "jvm-recent-companies-cards",
      div(
        cls := "jvm-recent-companies-card-img",
        renderCompanyPicture(company)
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
        renderOverview(company)
      ),
      renderAction(company)
    )

  private def renderCompanyPicture(company: Company) =
    img(
      cls := "img-fluid",
      src := company.image.getOrElse(Constants.companyLogoPlaceholder),
      alt := company.name
    )

  private def renderOverview(company: Company) =
    div(
      cls := "company-summary",
      renderDetail("location-dot", fullLocationString(company)),
      renderDetail("tags", company.tags.mkString(", "))
    )

  private def renderDetail(icon: String, value: String) =
    div(
      cls := "company-detail",
      i(cls := s"fa fa-$icon company-detail-icon"),
      p(cls := "company-detail-value", value)
    )

  private def fullLocationString(company: Company): String =
    (company.location, company.country) match {
      case (Some(l), Some(c)) => s"$l, $c"
      case (Some(l), None)    => l
      case (None, Some(c))    => c
      case (None, None)       => "N/A"
    }

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
