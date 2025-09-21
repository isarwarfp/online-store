package com.store.foundations

import cats.effect.{IOApp, MonadCancelThrow}
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.implicits.*
import scala.util.Random
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor

object Doobie extends IOApp.Simple:

    case class Student(id: Int, name: String)
    val xa: Transactor[IO] = Transactor.fromDriverManager[IO] (
        "org.postgresql.Driver", //JDBC Connector
        "jdbc:postgresql:demo",  // Database URL, if local so we shorten it from jdbc:postgresql:localhost:5432/demo
        "docker",                // username
        "docker"                 // password
    )

    def findAllStudents: IO[List[Student]] = {
        val query = sql"select * from students".query[Student]
        val action = query.to[List]
        action.transact(xa)
    }

    def saveStudent(s: Student): IO[Int] = {
        val query = sql"insert into students (id, name) values (${s.id}, ${s.name})"
        val action = query.update.run
        action.transact(xa)
    }

    def findStudentByInitial(letter: String): IO[List[Student]] = {
        val selectPart = fr"select id, name"
        val fromPart = fr"from students"
        val wherePart = fr"where left(name, 1) = $letter"
        val statement = selectPart ++ fromPart ++ wherePart
        
        val action = statement.query[Student].to[List]
        action.transact(xa)
    }

    // Organise Code, here by making Type Class
    // repository: Tagless Final
    trait Students[F[_]]:
        def findById(id: Int): F[Option[Student]]
        def findAll: F[List[Student]]
        def create(name: String): F[Int]
    
    object Students:
        // Here we need sort of Type Bounce for F
        def mk[F[_]: MonadCancelThrow](xa: Transactor[F]): Students[F] = new Students {
            def findById(id: Int): F[Option[Student]] = 
                sql"select * from students where id = $id".query[Student].option.transact(xa)
            def findAll: F[List[Student]] = sql"select * from students".query[Student].to[List].transact(xa)
            def create(name: String): F[Int] = 
                sql"insert into students (name) values ($name)".update.withUniqueGeneratedKeys[Int]("id").transact(xa)
        }

    // How to use that
    val postgresResource = for {
        ec <- ExecutionContexts.fixedThreadPool[IO](16)
        xa <- HikariTransactor.newHikariTransactor[IO]("org.postgresql.Driver", "jdbc:postgresql:demo", "docker", "docker", ec)
    } yield xa

    // Create Small Program
    val program = postgresResource.use { xa =>
        val repo = Students.mk[IO](xa)
        for {
            id <- repo.create("Muhammad")
            m <- repo.findById(id)
            _ <- IO.println(s"New Student: $m")
        } yield ()
    }

    def run: IO[Unit] = 
        val student: Student = Student(Random.nextInt, Random.nextString(5))
        //saveStudent(student) *> findAllStudents.map(println)
        // findStudentByInitial("U").map(println)
        program
