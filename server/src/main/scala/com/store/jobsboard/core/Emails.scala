package com.store.jobsboard.core

import cats.effect.*
import cats.implicits.*
import com.store.jobsboard.config.EmailServiceConfig
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.internet.InternetAddress
import javax.mail.Message
import javax.mail.Transport

trait Emails[F[_]]:
    def send(to: String, subject: String, content: String): F[Unit]
    def sendRecoveryToken(to: String, token: String): F[Unit]

class LiveEmails[F[_]: MonadCancelThrow](emailServiceConfig: EmailServiceConfig) extends Emails[F]:
    private val host = emailServiceConfig.host
    private val port = emailServiceConfig.port
    private val user = emailServiceConfig.user
    private val pass = emailServiceConfig.pass
    private val frontEndUrl = emailServiceConfig.frontEndUrl

    override def send(to: String, subject: String, content: String): F[Unit] = 
        val emailResource = for {
            prop <- properties
            auth <- authenticator
            session <- createSession(prop, auth)
            email <- createEmail(session)("imran.fp@outlook.com", to, subject, content)
        } yield email

        emailResource.use(email => Transport.send(email).pure[F])
    override def sendRecoveryToken(to: String, token: String): F[Unit] = 
        val subject = "Password Recovery"
        val content = s"Click <a href='$frontEndUrl/reset-password?token=$token'>here</a> to reset your password"
        send(to, subject, content)

    val properties: Resource[F, Properties] = {
        val prop = new Properties()
        prop.put("mail.smtp.auth", "true")
        prop.put("mail.smtp.starttls.enable", "true")
        prop.put("mail.smtp.host", host)
        prop.put("mail.smtp.port", port.toString)
        prop.put("mail.smtp.ssl.trust", host)
        Resource.pure(prop)
    }

    val authenticator: Resource[F, Authenticator] =
        Resource.pure(new Authenticator():
            override def getPasswordAuthentication(): PasswordAuthentication =
                new PasswordAuthentication(user, pass)
        )
    
    def createSession(prop: Properties, auth: Authenticator): Resource[F, Session] =
        Resource.pure(Session.getInstance(prop, auth))
    

    def createEmail(session: Session)(from: String, to: String, subject: String, content: String): Resource[F, MimeMessage] =
        val email = new MimeMessage(session)
        email.setFrom(new InternetAddress(from))
        email.setRecipients(Message.RecipientType.TO, to)
        email.setSubject(subject)
        email.setContent(content, "text/html")
        Resource.pure(email)    
    
object LiveEmails:
    def apply[F[_]: MonadCancelThrow](emailServiceConfig: EmailServiceConfig): F[LiveEmails[F]] = 
        new LiveEmails[F](emailServiceConfig).pure[F]