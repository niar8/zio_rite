package com.rite.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.common.Constants
import com.rite.core.BackendClient
import com.rite.core.ZJS.*
import com.rite.domain.data.InviteNamedRecord
import com.rite.http.requests.InviteRequest
import org.scalajs.dom
import org.scalajs.dom.HTMLDivElement
import zio.*

object InviteActions {
  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "profile-section",
      h3(span("Invites")),
      onMountCallback(_ => refreshInviteList().emitTo(inviteListBus)),
      children <-- inviteListBus.events.map(_.sortBy(_.companyName).map(renderInviteSection))
    )

  private val inviteListBus: EventBus[List[InviteNamedRecord]] = EventBus()

  private def refreshInviteList(): RIO[BackendClient, List[InviteNamedRecord]] =
    useBackend(_.invite.getByUserIdEndpoint(payload = ()))

  private def renderInviteSection(
      record: InviteNamedRecord
  ): ReactiveHtmlElement[HTMLDivElement] = {
    val emailListVar: Var[Array[String]]   = Var(Array.empty)
    val maybeErrorVar: Var[Option[String]] = Var(None)

    val inviteSubmitter: Observer[Unit] = Observer[Unit] { _ =>
      val emailList = emailListVar.now().toList
      if (emailList.exists(!_.matches(Constants.emailRegex)))
        maybeErrorVar.set(Some("At least one email is invalid"))
      else {
        val req = InviteRequest(record.companyId, emails = emailList)

        val refreshProgram: RIO[BackendClient, List[InviteNamedRecord]] =
          useBackend(_.invite.inviteEndpoint(payload = req)) *> refreshInviteList()

        maybeErrorVar.set(None)
        refreshProgram.emitTo(inviteListBus)
      }
    }

    div(
      cls := "invite-section",
      h5(span(record.companyName)),
      p(s"${record.nInvites} invites left"),
      textArea(
        cls         := "invites-area",
        placeholder := "Enter emails, one per line",
        onInput.mapToValue.map(_.split("\n").map(_.trim).filter(_.nonEmpty)) --> emailListVar.writer
      ),
      button(
        `type` := "button",
        cls    := "btn btn-primary",
        "Invite",
        onClick.mapToUnit --> inviteSubmitter
      ),
      child.maybe <-- maybeErrorVar.signal.map(maybeRenderError)
    )
  }

  private def maybeRenderError(
      maybeError: Option[String]
  ): Option[ReactiveHtmlElement[HTMLDivElement]] =
    maybeError.map { div(cls := "page-status-errors", _) }
}
