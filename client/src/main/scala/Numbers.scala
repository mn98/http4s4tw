import calico.dsl.io.*
import calico.syntax.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.concurrent.*
import fs2.Stream
import fs2.text
import org.http4s.client.Client
import org.http4s.headers.Accept
import org.http4s.implicits.uri
import org.http4s.{Header, Method, Request, Status}
import org.scalajs.dom.HTMLElement
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

  def streamer(client: Client[IO]): Resource[IO, HTMLElement] =
    SignallingRef[IO].of("???").product(SignallingRef[IO].of(false)).toResource.flatMap {
      (number, streaming) =>
        div(
          p(
            button(
              (Stream.eval(streaming.get) ++ streaming.discrete).map { streaming =>
                s"Click to ${if (streaming) "stop streaming" else "stream numbers"}"
              },
              onClick --> {
                _.foreach { _ =>
                  streaming.modify { streaming =>
                    if (streaming) {
                      !streaming -> client.run(stop).use { response =>
                        response.status match {
                          case Status.Ok =>
                            number.set("???")
                          case notOk =>
                            IO(println(s"Failed with status: $notOk")) >> number.set("???")
                        }
                      }
                    } else {
                      !streaming -> client.run(start).use { response =>
                        response.status match {
                          case Status.Ok =>
                            response
                              .body
                              .through(text.utf8.decode)
                              .foreach(number.set)
                              .compile
                              .drain
                              .background
                              .use(_.void)
                          case notOk =>
                            IO(println(s"Failed with status: $notOk")) >> number.set("???")
                        }
                      }
                    }
                  }.flatten
                }
              }
            ),
            b(number.discrete)
          )
        )
    }

}
