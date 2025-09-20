package com.store.jobsboard.playground

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Message
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import cats.effect.{IO, IOApp}
import com.store.jobsboard.core.LiveEmails
import com.store.jobsboard.config.EmailServiceConfig

class EmailPlayground:
    def main(args: Array[String]): Unit =
        // configs
        val host = "smtp.ethereal.email"
        val port = 587
        val username = "fletcher.goodwin73@ethereal.email"
        val password = "v1DwqQ4JGAtkbJGSjw"

        // properties
        val properties = new Properties()
        properties.put("mail.smtp.auth", "true")
        properties.put("mail.smtp.starttls.enable", "true")
        properties.put("mail.smtp.host", host)
        properties.put("mail.smtp.port", port.toString)

        // authentication
        val authenticator = new Authenticator():
            override def getPasswordAuthentication(): PasswordAuthentication =
                new PasswordAuthentication(username, password)

        // session
        val session = Session.getInstance(properties, authenticator)

        // email itself
        val subject = "Test Email"
        val body = "This is a test email"
        
        // Mime message
        val email = new MimeMessage(session)
        email.setFrom(new InternetAddress("imran.fp@outlook.com"))
        email.setRecipient(Message.RecipientType.TO, new InternetAddress("imran.fp.user@outlook.com"))
        email.setSubject(subject)
        email.setText(body)
    
        // transport
        val transport = session.getTransport("smtp")
        transport.connect(host, port, username, password)
        transport.sendMessage(email, email.getAllRecipients())
        transport.close()

object EmailPlayground extends IOApp.Simple:
    override def run: IO[Unit] = for {
        _ <- IO.println("🚀 Starting LiveEmails effectful example...")
        emails <- LiveEmails[IO](
            EmailServiceConfig(
                host = "smtp.ethereal.email", 
                port = 587, 
                user = "fletcher.goodwin73@ethereal.email", 
                pass = "v1DwqQ4JGAtkbJGSjw", 
                frontEndUrl = "http://localhost:8080")
        )
        _ <- IO.println("📧 Sending test email...")
        _ <- emails.send("imran.fp.user@outlook.com", "Check Effectful Email", "This is a test email sent using LiveEmails!")
        _ <- IO.println("✅ Email sent successfully!")
        
        _ <- IO.println("🔐 Sending recovery token email...")
        _ <- emails.sendRecoveryToken("imran.fp.user@outlook.com", "abc123token456")
        _ <- IO.println("✅ Recovery email sent successfully!")
    } yield ()

