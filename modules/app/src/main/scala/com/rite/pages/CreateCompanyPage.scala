package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.common.Constants
import com.rite.core.ZJS.*
import com.rite.http.requests.CreateCompanyRequest
import org.scalajs.dom
import org.scalajs.dom.*
import zio.ZIO

final case class CreateCompanyState(
    name: String = "",
    url: String = "",
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = Nil,
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  override def errorList: List[Option[String]] = List(
    Option.when(name.isEmpty)("The company name cannot be empty!"),
    Option.when(!url.matches(Constants.urlRegex))("The company URL is invalid")
  ) ++ upstreamStatus.map(_.left.toOption).toList

  override def maybeSuccess: Option[String] =
    upstreamStatus.flatMap(_.toOption)

  def toRequest: CreateCompanyRequest =
    CreateCompanyRequest(
      name,
      url,
      location,
      country,
      industry,
      image,
      Option(tags).filter(_.nonEmpty)
    )
}

object CreateCompanyPage extends FormPage[CreateCompanyState]("Create company") {
  override protected def basicState: CreateCompanyState = CreateCompanyState()

  override protected def renderChildren(): List[ReactiveHtmlElement[HTMLElement]] =
    List(
      renderInput(
        name = "Name",
        uid = "company-name",
        kind = "text",
        isRequired = true,
        plcHolder = "ACME Inc.",
        updateFn = (s, n) => s.copy(name = n, showStatus = false)
      ),
      renderInput(
        name = "URL",
        uid = "company-url",
        kind = "text",
        isRequired = true,
        plcHolder = "https://acme.com",
        updateFn = (s, u) => s.copy(url = u, showStatus = false)
      ),
      renderLogoUpload(
        name = "Logo",
        uid = "company-logo",
        isRequired = false
      ),
      img(
        src <-- stateVar.signal.map(_.image.getOrElse(""))
      ),
      renderInput(
        name = "Location",
        uid = "company-location",
        kind = "text",
        isRequired = false,
        plcHolder = "Somewhere",
        updateFn = (s, l) => s.copy(location = Some(l), showStatus = false)
      ),
      renderInput(
        name = "Country",
        uid = "company-country",
        kind = "text",
        isRequired = false,
        plcHolder = "Some country",
        updateFn = (s, c) => s.copy(country = Some(c), showStatus = false)
      ),
      renderInput(
        name = "Industry",
        uid = "company-industry",
        kind = "text",
        isRequired = false,
        plcHolder = "medicine",
        updateFn = (s, i) => s.copy(industry = Some(i), showStatus = false)
      ),
      renderInput(
        name = "Tags (separated by comma)",
        uid = "company-tags",
        kind = "text",
        isRequired = false,
        plcHolder = "Scala, ZIO, FP",
        updateFn = (s, ts) => s.copy(tags = ts.split(',').map(_.trim).toList, showStatus = false)
      ),
      button(
        `type` := "button",
        "Create company",
        onClick.preventDefault.mapTo(stateVar.now()) --> submitter
      )
    )

  private def renderLogoUpload(
      name: String,
      uid: String,
      isRequired: Boolean
  ): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "row",
      div(
        cls := "col-md-12",
        div(
          cls := "form-input",
          label(
            forId := uid,
            cls   := "form-label",
            if (isRequired) span("*") else span(),
            name
          ),
          input(
            `type` := "file",
            cls    := "form-control",
            idAttr := uid,
            accept := "image/*",
            onChange.mapToFiles --> fileUploader
          )
        )
      )
    )

  private val fileUploader = (files: List[File]) => {
    val maybeFile: Option[File] = files.headOption.filter(_.size > 0)
    maybeFile.foreach { file =>
      val reader = new FileReader
      reader.onload = _ => {
        val fakeImage = document.createElement("img").asInstanceOf[HTMLImageElement]
        fakeImage.addEventListener(
          "load",
          _ => {
            val canvas  = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
            val context = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

            val (width, height) = computeDimensions(fakeImage.width, fakeImage.height)
            canvas.width = width
            canvas.height = height

            context.drawImage(fakeImage, offsetX = 0, offsetY = 0, width, height)
            stateVar.update(_.copy(image = Some(canvas.toDataURL(file.`type`))))
          }
        )
        fakeImage.src = reader.result.toString
      }
      reader.readAsDataURL(file)
    }
  }

  private def computeDimensions(width: Int, height: Int): (Int, Int) =
    if (width >= height) {
      val ratio = width * 1.0 / 256
      val newW  = width / ratio
      val newH  = height / ratio
      (newW.toInt, newH.toInt)
    } else {
      val (newH, newW) = computeDimensions(height, width)
      (newW, newH)
    }

  private val submitter = Observer[CreateCompanyState] { state =>
    if (state.hasErrors)
      stateVar.update(_.copy(showStatus = true))
    else
      useBackend(
        _.company.createEndpoint(payload = state.toRequest)
      ).as {
        stateVar.update(
          _.copy(
            showStatus = true,
            upstreamStatus = Some(Right("Company created! Check it out in companies list."))
          )
        )
      }.tapError { e =>
        ZIO.succeed {
          stateVar.update(_.copy(showStatus = true, upstreamStatus = Some(Left(e.getMessage))))
        }
      }.runJs
  }
}
