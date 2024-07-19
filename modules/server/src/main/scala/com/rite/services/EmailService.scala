package com.rite.services

import com.rite.config.{Configs, EmailServiceConfig}
import zio.*

import java.util.Properties
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}

trait EmailService {
  def sendEmail(to: String, subject: String, content: String): Task[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit]
}

class EmailServiceLive private (config: EmailServiceConfig) extends EmailService {
  private val FROM = "rite_test@test.com"

  override def sendEmail(to: String, subject: String, content: String): Task[Unit] =
    for {
      props   <- propsResource
      session <- createSession(props)
      message <- createMessage(session)(FROM, to, subject, content)
    } yield Transport.send(message)

  override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] = {
    val subject = "This is password recovery"
    val content =
      s"""
         |<div style="
         |border: 1px solid black;
         |padding: 20px;
         |font-family: sans-serif;
         |line-height: 2;
         |font-size: 20px;
         |">
         |<h1>RITE Company password recovery</h1>
         |<p>Your password recovery token is: <strong>$token</strong></p>
         |</div>
         |""".stripMargin
    sendEmail(to, subject, content)
  }

  private val propsResource: Task[Properties] = ZIO.succeed {
    val props = new Properties
    props.put("mail.smtp.auth", true)
    props.put("mail.smtp.starttls.enable", "true")
    props.put("mail.smtp.host", config.host)
    props.put("mail.smtp.port", config.port)
    props.put("mail.smtp.ssl.trust", config.host)
    props
  }

  private def createSession(prop: Properties): Task[Session] =
    ZIO.attempt {
      Session.getInstance(
        prop,
        new Authenticator {
          override def getPasswordAuthentication: PasswordAuthentication =
            new PasswordAuthentication(config.user, config.pass)
        }
      )
    }

  private def createMessage(session: Session)(
      from: String,
      to: String,
      subject: String,
      content: String
  ): Task[MimeMessage] = ZIO.succeed {
    val message = new MimeMessage(session)
    message.setFrom(from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=UTF-8")
    message
  }
}

object EmailServiceLive {
  val layer: URLayer[EmailServiceConfig, EmailService] = ZLayer {
    ZIO.serviceWith[EmailServiceConfig](new EmailServiceLive(_))
  }

  val configuredLayer: TaskLayer[EmailService] =
    Configs.makeConfigLayer[EmailServiceConfig]("rite.email") >>> layer
}

object EmailServiceDemo extends ZIOAppDefault {
  val program: RIO[EmailService, Unit] = for {
    emailService <- ZIO.service[EmailService]
//    _ <- emailService.sendEmail("spiderman@rite.com", "Hi from ZIO", "This is a email test")
    _ <- emailService.sendPasswordRecoveryEmail("spiderman@rite.com", "ABCDF12")
    _ <- Console.printLine("Email sent")
  } yield ()

  override def run: Task[Unit] =
    program.provide(EmailServiceLive.configuredLayer)
}
