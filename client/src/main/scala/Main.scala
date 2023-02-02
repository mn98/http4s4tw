import cats.effect.kernel.Sync
import cats.effect.std.{Dispatcher, Queue}
import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import fs2.Stream
import org.http4s.client.Client
import org.http4s.dom.FetchClientBuilder
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
            onRender.traverse_ { onRender =>
              log(s"Performing post-rendering operations") >> onRender.void
            } >>
            log(s"Rendered $parentName -> $childName")
        }
          .whenA(parent != null)
      }
  }

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

  def newNode[F[_]](parentName: String, newNodeName: String)(using F: Sync[F]): F[Node] = F.delay({
    val parent = dom.document.getElementById(parentName)
    val container = dom.document.createElement(newNodeName)
    parent.appendChild(container)
    container
  })

  private val renderCalicoCounter = {
    import calico.*
    import calico.html.io.{*, given}
    import calico.syntax.*
    import calico.unsafe.given

    newNode[IO]("app", "calico-counter").flatMap { node =>
      val app = div(
        h1("Let's count!"),
        CalicoCounter.create("Sheep", initialStep = 3)
      )
      app.renderInto(node.asInstanceOf[fs2.dom.Node[IO]]).allocated
    }
  }

  val client: Client[IO] = FetchClientBuilder[IO].create

  private def renderCalicoHelloWorld(client: Client[IO]) = {
    import calico.*
    import calico.html.io.{*, given}
    import calico.syntax.*
    import calico.unsafe.given

    newNode[IO]("app", "calico-hello-world").flatMap { node =>
      val app = div(
        h1("Server demo!"),
        Hello.world(client)
      )
      app.renderInto(node.asInstanceOf[fs2.dom.Node[IO]]).allocated
    }
  }

  def renderCalicoNumbers(client: Client[IO]) = {
    import calico.*
    import calico.html.io.{*, given}
    import calico.syntax.*
    import calico.unsafe.given

    newNode[IO]("app", "calico-number-stream").flatMap { node =>
      val app = div(
        h1("Streaming demo!"),
        Numbers.streamer(client)
      )
      app.renderInto(node.asInstanceOf[fs2.dom.Node[IO]]).allocated
    }
  }

  override def run: IO[Unit] = {
    Dispatcher.sequential[IO].use { dispatcher =>
      Stream.eval(Queue.unbounded[IO, String]).flatMap { logs =>

        val log: String => IO[Unit] = logs.offer

        val program: Stream[IO, Unit] = Stream.exec {
          log("Hello, World") >>
            displayHelloWorld.void >>
            createAppDiv.void >>
            Logger(dispatcher, log).flatMap { logger =>
              render("app", "click-counter", ClickCounter(logger), log) >>
                render("app", "number-stream", SlinkyNumbers(client, dispatcher, logger), log)
            } >>
            renderCalicoCounter.void >>
            renderCalicoHelloWorld(client).void >>
            renderCalicoNumbers(client).void
        }

        val logging = Stream.fromQueueUnterminated(logs).map(s => println(s"log: $s"))

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
