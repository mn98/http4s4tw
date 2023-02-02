import calico.*
import calico.html.io.{*, given}
import calico.unsafe.given
import calico.syntax.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.*
import fs2.dom.HtmlDivElement
import org.http4s.client.Client
import org.http4s.headers.Accept
import org.http4s.implicits.uri
import org.http4s.{Header, Method, Request, Status}
import org.typelevel.ci.CIStringSyntax

object Numbers {

  private val restUrl = uri"http://localhost:8080"

  private val start: Request[IO] =
    Request()
      .withMethod(Method.GET)
      .withUri(restUrl / "api" / "numbers")
      .putHeaders(Accept.parse("text/plain"))

  private val stop: Request[IO] =
    Request()
      .withMethod(Method.GET)
      .withUri(restUrl / "api" / "numbers" / "stop")
      .putHeaders(Accept.parse("text/plain"))

  def streamer(client: Client[IO]): Resource[IO, HtmlDivElement[IO]] =
    SignallingRef[IO].of("???").product(SignallingRef[IO].of(false)).toResource.flatMap {
      (number, streaming) =>
        div(
          p(
            button(
              (Stream.eval(streaming.get) ++ streaming.discrete).map { streaming =>
                s"Click to ${if (streaming) "stop streaming" else "stream numbers"}"
              }.holdOptionResource,
              onClick --> {
                _.foreach { _ =>
                  streaming.modify { streaming =>
                    if (streaming) {
                      !streaming -> client.run(stop).use { response =>
                        response.status match {
                          case Status.Ok =>
                            number.set("???")
                          case notOk =>
                            IO.println(s"Failed with status: $notOk") >> number.set("???")
                        }
                      }
                    } else {
                      !streaming -> client.run(start).use { response =>
                        response.status match {
                          case Status.Ok =>
                            response
                              .body
                              .through(text.utf8.decode)
                              .foreach(i => IO.println(s"Client received $i") >> number.set(i))
                              .compile
                              .drain
                              .background
                              .use(_.void)
                          case notOk =>
                            IO.println(s"Failed with status: $notOk") >> number.set("???")
                        }
                      }
                    }
                  }.flatten
                }
              }
            ),
            b(number.map(x => x))
          )
        )
    }

}
