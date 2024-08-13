package com.rite.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.common.Constants
import com.rite.domain.data.Company
import org.scalajs.dom
import org.scalajs.dom.{HTMLDivElement, HTMLImageElement}

object CompanyComponents {
  def renderCompanyPicture(company: Company): ReactiveHtmlElement[HTMLImageElement] =
    img(
      cls := "img-fluid",
      src := company.image.getOrElse(Constants.companyLogoPlaceholder),
      alt := company.name
    )

  def renderOverview(company: Company): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "company-summary",
      renderDetail("location-dot", fullLocationString(company)),
      renderDetail("tags", company.tags.mkString(", "))
    )

  private def renderDetail(
      icon: String,
      value: String
  ): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "company-detail",
      i(cls := s"fa fa-$icon company-detail-icon"),
      p(
        cls := "company-detail-value",
        value
      )
    )

  private def fullLocationString(company: Company): String =
    (company.location, company.country) match {
      case (Some(l), Some(c)) => s"$l, $c"
      case (Some(l), None)    => l
      case (None, Some(c))    => c
      case (None, None)       => "N/A"
    }
}
