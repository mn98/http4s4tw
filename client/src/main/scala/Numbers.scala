import calico.dsl.io.*
import calico.syntax.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.concurrent.*
import fs2.text
import org.http4s.client.Client
import org.http4s.headers.Accept
import org.http4s.implicits.uri
import org.http4s.{Header, Method, Request, Status}
import org.scalajs.dom.HTMLElement
import org.typelevel.ci.CIStringSyntax

object Numbers {

  private val restUrl = uri"http://localhost:8080"

  private val get: Request[IO] =
    Request()
      .withMethod(Method.GET)
      .withUri(restUrl / "api" / "numbers")
      .putHeaders(Accept.parse("text/plain"))

  def streamer(client: Client[IO]): Resource[IO, HTMLElement] =
    SignallingRef[IO].of("???").toResource.flatMap { number =>
      div(
        p(
          button(
            "Click to stream numbers",
            onClick --> {
              _.foreach { _ =>
                client.run(get).use { response =>
                  response.status match {
                    case Status.Ok =>
                      response
                        .body
                        .through(text.utf8.decode)
                        .foreach(number.set)
                        .compile
                        .drain
                    case notOk =>
                      IO(println(s"Failed with status: $notOk")) >> number.set("???")
                  }
                }
              }
            }
          ),
          span(number.discrete)
        ),
      )
    }

}
