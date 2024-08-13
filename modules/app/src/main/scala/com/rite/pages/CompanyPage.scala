package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.components.CompanyComponents.*
import com.rite.core.ZJS.*
import com.rite.domain.data.*
import org.scalajs.dom.HTMLDivElement

import java.time.Instant

object CompanyPage {
  def apply(id: Long): ReactiveHtmlElement[HTMLDivElement] =
    div(
      onMountCallback { _ =>
        useBackend(_.company.getByIdEndpoint(payload = id.toString)).emitTo(fetchCompanyBus)
      },
      children <-- status.map {
        case Status.LOADING     => List(div("loading..."))
        case Status.NOT_FOUND   => List(div("company not found"))
        case Status.OK(company) => render(company, reviewsSignal(id))
      },
      child <-- reviewsSignal(id).map(_.toString)
    )

  private val fetchCompanyBus: EventBus[Option[Company]] = EventBus[Option[Company]]()

  private enum Status {
    case LOADING
    case NOT_FOUND
    case OK(company: Company)
  }

  private val status: Signal[Status] = fetchCompanyBus.events.scanLeft(Status.LOADING) {
    (_, maybeCompany) =>
      maybeCompany match {
        case None          => Status.NOT_FOUND
        case Some(company) => Status.OK(company)
      }
  }

  private def reviewsSignal(companyId: Long): Signal[List[Review]] =
    fetchCompanyBus.events
      .flatMap {
        case None =>
          EventStream.empty
        case Some(company) =>
          val reviewsBus = EventBus[List[Review]]()
          useBackend {
            _.review.getByCompanyIdEndpoint(payload = companyId)
          }.emitTo(reviewsBus)
          reviewsBus.events
      }
      .scanLeft(List[Review]())((_, list) => list)

  private def render(
      company: Company,
      reviewsSignal: Signal[List[Review]]
  ): List[ReactiveHtmlElement[HTMLDivElement]] = List(
    div(
      cls := "row jvm-companies-details-top-card",
      div(
        cls := "col-md-12 p-0",
        div(
          cls := "jvm-companies-details-card-profile-img",
          renderCompanyPicture(company)
        ),
        div(
          cls := "jvm-companies-details-card-profile-title",
          h1(company.name),
          div(
            cls := "jvm-companies-details-card-profile-company-details-company-and-location",
            renderOverview(company)
          )
        ),
        div(
          cls := "jvm-companies-details-card-apply-now-btn",
          button(
            `type` := "button",
            cls    := "btn btn-warning",
            "Add a review"
          )
        )
      )
    ),
    div(
      cls := "container-fluid",
      renderCompanySummary, // TODO
      children <-- reviewsSignal.map(_.map(renderReview)),
      div(
        cls := "container",
        div(
          cls := "rok-last",
          div(
            cls := "row invite-row",
            div(
              cls := "col-md-6 col-sm-6 col-6",
              span(
                cls := "rock-apply",
                p("Do you represent this company?"),
                p("Invite people to leave reviews.")
              )
            ),
            div(
              cls := "col-md-6 col-sm-6 col-6",
              a(
                href   := company.url,
                target := "blank",
                button(`type` := "button", cls := "rock-action-btn", "Invite people")
                // todo invite new people
              )
            )
          )
        )
      )
    )
  )

  private def renderCompanySummary: ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        div(
          cls := "company-description",
          "TODO company summary"
        )
      )
    )

  private def renderReview(review: Review): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        // TODO add a highlight if this is "your" review
        div(
          cls := "company-description",
          div(
            cls := "review-summary",
            renderReviewDetail("Would Recommend", review.wouldRecommend),
            renderReviewDetail("Management", review.management),
            renderReviewDetail("Culture", review.culture),
            renderReviewDetail("Salary", review.salary),
            renderReviewDetail("Benefits", review.benefits)
          ),
          // TODO parse this Markdown
          div(
            cls := "review-content",
            review.review
          ),
          div(cls := "review-posted", "Posted (TODO) a million years ago")
        )
      )
    )

  private def renderReviewDetail(
      detail: String,
      score: Int
  ): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "review-detail",
      span(cls := "review-detail-name", s"$detail: "),
      (1 to score).toList.map(_ =>
        svg.svg(
          svg.cls     := "review-rating",
          svg.viewBox := "0 0 32 32",
          svg.path(
            svg.d := "m15.1 1.58-4.13 8.88-9.86 1.27a1 1 0 0 0-.54 1.74l7.3 6.57-1.97 9.85a1 1 0 0 0 1.48 1.06l8.62-5 8.63 5a1 1 0 0 0 1.48-1.06l-1.97-9.85 7.3-6.57a1 1 0 0 0-.55-1.73l-9.86-1.28-4.12-8.88a1 1 0 0 0-1.82 0z"
          )
        )
      )
    )
}
