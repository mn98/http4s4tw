import cats.effect.{IO, IOApp, Resource}
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.Stream
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
import fs2.concurrent.SignallingRef

import concurrent.duration.DurationInt

val helloWorldService: HttpRoutes[IO] = HttpRoutes.of[IO] {
  case GET -> Root / "hello" / name =>
    Ok(
      s"Hello, $name.",
      Header.Raw(ci"Access-Control-Allow-Origin", "*")
    )
}

private val numbers = Stream.unfold[IO, Int, Int](0)(i => Some((i + 1, i + 1))).map(_.toString).metered(1.second)

def streamingNumberService(stopStreaming: SignallingRef[IO, Boolean]): HttpRoutes[IO] =
  HttpRoutes.of[IO] {
    case GET -> Root / "numbers" =>
      Ok(
        Stream.exec(stopStreaming.set(false)) ++ numbers.debug(i => s"Server streamed $i").interruptWhen(stopStreaming),
        Header.Raw(ci"Access-Control-Allow-Origin", "*")
      )
    case GET -> Root / "numbers" / "stop" =>
      Ok(
        Stream.exec(stopStreaming.set(true)) ++ Stream("Streaming stopped"),
        Header.Raw(ci"Access-Control-Allow-Origin", "*")
      )
  }

def httpApp(routes: HttpRoutes[IO]*): HttpApp[IO] = Router(
  "/" -> helloWorldService,
  "/api" -> routes.foldLeft(helloWorldService)(_ <+> _)
).orNotFound

def server(app: HttpApp[IO]): Resource[IO, Server] = EmberServerBuilder
  .default[IO]
  .withHost(ipv4"0.0.0.0")
  .withPort(port"8080")
  .withHttpApp(app)
  .build

object Main extends IOApp.Simple {
  override def run: IO[Unit] =
    SignallingRef[IO].of(false).flatMap { stopStreaming =>
      server(httpApp(streamingNumberService(stopStreaming))).use(_ => IO.never)
    }
}
