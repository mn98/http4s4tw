import cats.effect.kernel.Sync
import cats.effect.std.{Dispatcher, Queue}
import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import fs2.Stream
import org.scalajs.dom
import org.scalajs.dom.Node
import slinky.core.facade.ReactElement
import slinky.web

object Main extends IOApp.Simple {

  def render[F[_]](
                    parentName: String,
                    childName: String,
                    child: ReactElement,
                    log: String => F[Unit],
                    onRender: Option[F[Unit]] = None,
                  )(
                    using F: Sync[F],
                  ): F[Unit] = {
    log(s"Attempting to render $childName within $parentName") >>
      F.delay(dom.document.getElementById(parentName)).flatMap {
        parent => {
          log(s"Found parent: $parentName") >>
            F.delay {
              val container = dom.document.createElement(childName)
              parent.appendChild(container)
              web.ReactDOM.render(child, container)
            } >>
            onRender.traverse_(_.void) >>
            log(s"Rendered $parentName -> $childName")
        }
          .whenA(parent != null)
      }
  }

  val logHelloWorld: IO[Unit] = IO(println(s"Hello, World"))

  val displayHelloWorld: IO[Node] = IO({
    val parNode = dom.document.createElement("p")
    val textNode = dom.document.createTextNode("Hello, World")
    parNode.appendChild(textNode)
    dom.document.body.appendChild(parNode)
  })

  val createAppDiv: IO[Node] = IO({
    val appDiv = dom.document.createElement("div")
    appDiv.id = "app"
    dom.document.body.appendChild(appDiv)
  })

  override def run: IO[Unit] = {
    Dispatcher[IO].use { dispatcher =>

      Stream.eval(Queue.unbounded[IO, String]).flatMap { log =>

        val logger: String => IO[Unit] = log.offer

        val program: Stream[IO, Unit] = Stream.exec {
          logHelloWorld >>
            displayHelloWorld.void >>
            createAppDiv.void >>
            Logger(dispatcher).flatMap { ceLogger =>
              render("app", "click-counter", ClickCounter(ceLogger), logger)
            }
        }

        val logging = Stream.fromQueueUnterminated(log).map(s => println(s"log: $s"))

        Stream(
          program,
          logging // this never terminates, allowing the dispatcher to live forever
        )
          .parJoinUnbounded
      }
        .compile
        .drain
    }
  }
}
