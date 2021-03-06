// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gem
package telnetd

import tuco._, Tuco._
import doobie.imports._
import org.flywaydb.core.Flyway
import scalaz._, scalaz.effect._, scalaz.concurrent.Task

/**
 * Entry point for running Gem with a telnet server. This will go away at some point and the telnet
 * server will be one of several services.
 */
object Main extends SafeApp {

  /** When we start the app with docker we pass arguments as environment variables. */
  val ENV_GEM_DB_URL : String = "GEM_DB_URL"
  val ENV_GEM_DB_USER: String = "GEM_DB_USER"
  val ENV_GEM_DB_PASS: String = "GEM_DB_PASS"

  /** Get an environment variable. */
  def getEnv(key: String, default: String): IO[String] =
    IO(sys.env.getOrElse(key, default))

  /** Construct a transactor with the give effect type. */
  def xa[M[_]: Monad: Capture: Catchable](url: String, user: String, pass: String): Transactor[M, _] =
    DriverManagerTransactor[M]("org.postgresql.Driver", url, user, pass)

  /** Run migrations. */
  def migrate(url: String, user: String, pass: String): IO[Int] =
    IO {
      val flyway = new Flyway()
      flyway.setDataSource(url, user, pass);
      flyway.migrate()
    }

  override def runc: IO[Unit] =
    for {
      url  <- getEnv(ENV_GEM_DB_URL,  "jdbc:postgresql:gem")
      user <- getEnv(ENV_GEM_DB_USER, "postgres")
      pass <- getEnv(ENV_GEM_DB_PASS,  "")
      _    <- IO.putStrLn(s"Connecting with URL $url, user $user, pass «hidden»")
      _    <- migrate(url, user, pass)
      sxa  = xa[SessionIO](url, user, pass)
      txa  = xa[Task     ](url, user, pass)
      _    <- Config(Interaction.main(sxa, txa), 6666).run(simpleServer)
    } yield ()

}
