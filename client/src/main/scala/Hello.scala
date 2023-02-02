import calico.*
import calico.html.io.{*, given}
import calico.unsafe.given
import calico.syntax.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.concurrent.*
import fs2.dom.HtmlDivElement
import fs2.text
import org.http4s.client.Client
import org.http4s.headers.Accept
import org.http4s.implicits.uri
import org.http4s.{Header, Method, Request, Status}
import org.typelevel.ci.CIStringSyntax

object Hello {

  private val restUrl = uri"http://localhost:8080"

  private def get(name: String): Request[IO] =
    Request()
      .withMethod(Method.GET)
      .withUri(restUrl / "hello" / name)
      .putHeaders(Accept.parse("text/plain"))

  def world(client: Client[IO]): Resource[IO, HtmlDivElement[IO]] =
    SignallingRef[IO].of("???").toResource.flatMap { name =>
      div(
        p(
          label("Name: "),
          input.withSelf { self =>
            (
              placeholder := "Enter your name here",
              onInput --> {
                _.foreach { _ =>
                  self.value.get.flatMap { nameEntered =>
                    client.run(get(nameEntered)).use { response =>
                      response.status match {
                        case Status.Ok =>
                          response
                            .body
                            .through(text.utf8.decode andThen text.lines)
                            .evalMap(name.set)
                            .compile
                            .drain
                        case notOk =>
                          IO(println(s"Failed with status: $notOk")) >> name.set("???")
                      }
                    }
                  }
                }
              }
            )
          }
        ),
        span(name.map(x => x))
      )
    }

}
