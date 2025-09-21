package com.store.foundations

import cats.*
import cats.effect.{IO, IOApp}
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.*
import org.typelevel.ci.CIString

import java.util.UUID

object Http4s extends IOApp.Simple:
  // Simulate an Http Server with `students` and `courses`
  type Student = String
  case class Instructor(fn: String, ln: String)
  case class Course(id: String, title: String, year: Int, students: List[Student], instructorName: String)
  // Query Parameter
  object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor")

  object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")
  // Routes
  // GET localhost:8080/courses?instructor=Martin%20Odersky
  def courseRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(instructor) +& YearQueryParamMatcher(mayBeYear) =>
        val c = CourseRepository.findCourseByInstructor(instructor)
        mayBeYear match
          case Some(year) => year.fold(
            _ => BadRequest("Parameter Year is invalid"),
            y => Ok(c.filter(_.year == y).asJson)
          )
          case None => Ok(c.asJson)
      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCourseById(courseId).map(_.students) match
          case Some(students) => Ok(students.asJson, Header.Raw(CIString("custom-header"), "Imran"))
          case None => NotFound(s"No course found with id $courseId")
    }
  }

  def healthEndpoint[F[_]: Monad]: HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok("All going good.")
    }

  def allRoutes[F[_]: Monad]: HttpRoutes[F] = courseRoutes[F] <+> healthEndpoint[F]

  val rountesWithPrefixes = Router (
    "/api" -> courseRoutes[IO],
    "/private" -> healthEndpoint[IO]
  ).orNotFound

  // Using Tagless Final
  object CourseRepository:
    private val course = Course("f85027a0-39b4-472c-bb90-b4f3a291b64f", "Math", 2022, List("imran", "umar"), "Martin")
    private val courses = Map(course.id -> course)

    // API
    def findCourseById(uuid: UUID): Option[Course] = courses.get(uuid.toString)
    def findCourseByInstructor(name: String): List[Course] = courses.values.find(_.instructorName == name).toList

  override def run: IO[Unit] =
    //IO.println(UUID.randomUUID())
    EmberServerBuilder
    .default[IO]
    .withHttpApp(rountesWithPrefixes)
    .build // that will create resource
    .use( _ => IO.println("Server Started") *> IO.never)

