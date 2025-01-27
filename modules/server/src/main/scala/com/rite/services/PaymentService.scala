package com.rite.services

import com.rite.config.{Configs, StripeConfig}
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import com.stripe.Stripe as TheStripe
import com.stripe.net.Webhook
import scala.jdk.OptionConverters.*
import zio.*

trait PaymentService {
  def createCheckoutSession(
      invitePackId: Long,
      userName: String
  ): Task[Option[Session]]

  def handleWebhookEvent[A](
      signature: String,
      payload: String,
      action: String => Task[A]
  ): Task[Option[A]]
}

class PaymentServiceLive(stripeConfig: StripeConfig) extends PaymentService {
  override def createCheckoutSession(
      invitePackId: Long,
      userName: String
  ): Task[Option[Session]] =
    ZIO
      .attempt {
        val invoiceCreation = SessionCreateParams.InvoiceCreation
          .builder()
          .setEnabled(true)
          .build()
        val paymentIntentData = SessionCreateParams.PaymentIntentData
          .builder()
          .setReceiptEmail(userName)
          .build()
        val lineItem = SessionCreateParams.LineItem
          .builder()
          .setPrice(stripeConfig.price) // unique id of your Stripe product
          .setQuantity(1L)
          .build()

        SessionCreateParams
          .builder()
          .setMode(SessionCreateParams.Mode.PAYMENT)
          .setSuccessUrl(stripeConfig.successUrl)
          .setCancelUrl(stripeConfig.cancelUrl)
          .setCustomerEmail(userName)
          .setClientReferenceId(invitePackId.toString)
          .setInvoiceCreation(invoiceCreation)
          .setPaymentIntentData(paymentIntentData)
          .addLineItem(lineItem)
          .build()
      }
      .map(Session.create)
      .map(Option(_))
      .logError("Stripe session creation FAILED")
      .catchSome { case _ => ZIO.none }

  override def handleWebhookEvent[A](
      signature: String,
      payload: String,
      action: String => Task[A]
  ): Task[Option[A]] =
    ZIO
      .attempt { Webhook.constructEvent(payload, signature, stripeConfig.secret) }
      .flatMap { event =>
        event.getType match {
          case "checkout.session.completed" =>
            ZIO.foreach(
              event.getDataObjectDeserializer.getObject.toScala
                .map(_.asInstanceOf[Session])
                .map(_.getClientReferenceId())
            )(action)
          case _ =>
            ZIO.none
        }
      }
}

object PaymentServiceLive {
  val layer: RLayer[StripeConfig, PaymentService] = ZLayer {
    for {
      config <- ZIO.service[StripeConfig]
      _      <- ZIO.attempt(TheStripe.apiKey = config.key)
    } yield new PaymentServiceLive(config)
  }

  val configuredLayer: TaskLayer[PaymentService] =
    Configs.makeConfigLayer[StripeConfig]("rite.stripe") >>> layer
}
