import cats.effect.{IO, IOApp, Resource}
import cats.syntax.all.*
import org.http4s.*
import org.http4s.Method.GET
import org.http4s.Status.Ok
import org.http4s.Uri.Path.Root
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.{Router, Server}
import org.http4s.ember.server.*
import org.typelevel.ci.CIStringSyntax
import com.comcast.ip4s.*

val helloWorldService: HttpRoutes[IO] = HttpRoutes.of[IO] {
  case GET -> Root / "hello" / name =>
    Ok(
      s"Hello, $name.",
      Header.Raw(ci"Access-Control-Allow-Origin", "*")
    )
}

val httpApp: HttpApp[IO] = Router("/" -> helloWorldService).orNotFound

val server: Resource[IO, Server] = EmberServerBuilder
  .default[IO]
  .withHost(ipv4"0.0.0.0")
  .withPort(port"8080")
  .withHttpApp(httpApp)
  .build

object Main extends IOApp.Simple {
  override def run: IO[Unit] = server.use(_ => IO.never)
}
