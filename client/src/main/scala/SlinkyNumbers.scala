import cats.effect.IO
import cats.effect.std.Dispatcher
import fs2.text
import org.http4s.Request
import org.http4s.client.Client
import org.scalajs.dom
import slinky.core.FunctionalComponent
import slinky.core.facade.Hooks.{useEffect, useState}
import slinky.core.facade.ReactElement
import slinky.web.html.*
import org.http4s.headers.Accept
import org.http4s.implicits.uri
import org.http4s.{Header, Method, Request, Status}
import org.scalajs.dom.{Fetch, Headers, HttpMethod, RequestInit}
import scala.scalajs.js.Thenable.Implicits.*

import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.*

object SlinkyNumbers {

  case class Props(
                    client: Client[IO],
                    dispatcher: Dispatcher[IO],
                    logger: Logger
                  )

  def apply(
             client: Client[IO],
             dispatcher: Dispatcher[IO],
             logger: Logger
           ): ReactElement = component(Props(client, dispatcher, logger))

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

  private val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>

    val (number, setNumber) = useState("???")

    div(
      p("Stream to Slinky"),
      button(
        "Start",
        onClick := { () =>
          props.dispatcher.unsafeRunAndForget {
            props.client.run(start).use { response =>
              response.status match {
                case Status.Ok =>
                  response
                    .body
                    .through(text.utf8.decode)
                    .foreach(i => IO.println(s"Client received $i") >> IO(setNumber(i)))
                    .compile
                    .drain
                case notOk =>
                  IO.println(s"Failed with status: $notOk") >> IO(setNumber("???"))
              }
            }
          }
        }
      ),
      button(
        "Stop",
        onClick := { () =>
          props.dispatcher.unsafeRunAndForget {
            props.client.run(stop).use { response =>
              response.status match {
                case Status.Ok =>
                  IO(setNumber("???"))
                case notOk =>
                  IO.println(s"Failed with status: $notOk") >> IO(setNumber("???"))
              }
            }
          }
        }
      ),
      b(number),
      button(
        "Start with fetch",
        onClick := { () => {
          val localHeaders = new Headers()
          localHeaders.set("Content-Type", "text/plain")
          Fetch.fetch(
            "http://localhost:8080/api/numbers",
            new RequestInit {
              method = HttpMethod.GET
              headers = localHeaders
            })
            .flatMap(res => res.text())
            .map(data => setNumber(data))
        }: Unit
        }
      )
    )

  }

}