import calico.*
import calico.html.io.{*, given}
import calico.unsafe.given
import calico.syntax.*
import cats.effect.*
import cats.effect.std.{Hotswap, Supervisor}
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

  private val runStart: (Client[IO], SignallingRef[IO, String]) => IO[Unit] =
    (client, number) =>
      client.run(start).use { response =>
        response.status match {
          case Status.Ok =>
            response
              .body
              .through(text.utf8.decode)
              .debug(i => s"Client received $i")
              .foreach(number.set)
              .compile
              .drain
          case notOk =>
            IO.println(s"Failed with status: $notOk") >> number.set("???")
        }
      }

  private val runStop: (Client[IO], SignallingRef[IO, String]) => IO[Unit] =
    (client, number) =>
      client.run(stop).use { response =>
        response.status match {
          case Status.Ok =>
            number.set("???")
          case notOk =>
            IO.println(s"Failed with status: $notOk") >> number.set("???")
        }
      }

  def oneButtonParallelStreamer(client: Client[IO]): Resource[IO, HtmlDivElement[IO]] =
    SignallingRef[IO].of("???").product(SignallingRef[IO].of(false)).toResource.flatMap {
      (number, streaming) =>
        div(
          p(
            "One button parallel streamer: ",
            button(
              streaming.discrete.map(if _ then "Stop" else "Start").holdOptionResource,
              onClick --> {
                _.parEvalMap(2) { _ =>
                  streaming.modify { streaming =>
                    if (streaming) {
                      false -> runStop(client, number)
                    } else {
                      true -> runStart(client, number)
                    }
                  }.flatten
                }.drain
              }
            ),
            b(number),
          )
        )
    }

  def oneButtonSupervisedStreamer(client: Client[IO]): Resource[IO, HtmlDivElement[IO]] =
    Supervisor[IO].flatMap { supervisor =>
      SignallingRef[IO].of("???").product(SignallingRef[IO].of(false)).toResource.flatMap {
        (number, streaming) =>
          div(
            p(
              "One button supervised streamer: ",
              button(
                streaming.discrete.map(if _ then "Stop" else "Start").holdOptionResource,
                onClick --> {
                  _.foreach { _ =>
                    supervisor.supervise {
                      streaming.modify { streaming =>
                        if (streaming) {
                          false -> runStop(client, number)
                        } else {
                          true -> runStart(client, number)
                        }
                      }.flatten
                    }.void
                  }
                }
              ),
              b(number),
            )
          )
      }
    }

  def oneButtonHotswappedStreamer(client: Client[IO]): Resource[IO, HtmlDivElement[IO]] =
    Hotswap.create[IO, Unit].flatMap { hotswap =>
      SignallingRef[IO].of("???").product(SignallingRef[IO].of(false)).toResource.flatMap {
        (number, streaming) =>
          div(
            p(
              "One button hotswapped streamer: ",
              button(
                streaming.discrete.map(if _ then "Stop" else "Start").holdOptionResource,
                onClick --> {
                  _.foreach { _ =>
                    streaming.modify { streaming =>
                      if (streaming) {
                        false -> hotswap.swap {
                          runStop(client, number).background.void
                        }
                      } else {
                        true -> hotswap.swap {
                          runStart(client, number).background.void
                        }
                      }
                    }.flatten
                  }
                }
              ),
              b(number),
            )
          )
      }
    }

  def twoButtonStreamer(client: Client[IO]): Resource[IO, HtmlDivElement[IO]] =
    SignallingRef[IO].of("???").product(SignallingRef[IO].of(false)).toResource.flatMap {
      (number, streaming) =>
        div(
          p(
            "Two button streamer: ",
            button(
              "Start",
              hidden <-- streaming,
              onClick --> {
                _.foreach { _ =>
                  streaming.update(!_) >> runStart(client, number)
                }
              }
            ),
            button(
              "Stop",
              hidden <-- streaming.map(!_),
              onClick --> {
                _.foreach { _ =>
                  streaming.update(!_) >> runStop(client, number)
                }
              },
            ),
            b(number),
          )
        )
    }

}
