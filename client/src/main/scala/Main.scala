import calico.*
import calico.html.io.{*, given}
import calico.syntax.*
import calico.unsafe.given
import cats.effect.kernel.Sync
import cats.effect.std.Queue
import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import fs2.Stream
import org.http4s.client.Client
import org.http4s.dom.FetchClientBuilder
import org.scalajs.dom
import org.scalajs.dom.Node

private val helloWorld: IO[Node] = IO({
  val parNode = dom.document.createElement("p")
  val textNode = dom.document.createTextNode("Hello, World")
  parNode.appendChild(textNode)
  dom.document.body.appendChild(parNode)
})

private val createAppDiv: IO[Node] = IO({
  val appDiv = dom.document.createElement("div")
  appDiv.id = "app"
  dom.document.body.appendChild(appDiv)
})

private def newNode[F[_]](parentName: String, newNodeName: String)(using F: Sync[F]): F[Node] = F.delay({
  val parent = dom.document.getElementById(parentName)
  val container = dom.document.createElement(newNodeName)
  parent.appendChild(container)
  container
})

private val calicoColours = {
  newNode[IO]("app", "calico-colours").flatMap { node =>
    val app = div(
      h1("All the colours!"),
      Colours.create
    )
    app.renderInto(node.asInstanceOf[fs2.dom.Node[IO]]).allocated
  }
}

private val calicoCounter = {
  newNode[IO]("app", "calico-counter").flatMap { node =>
    val app = div(
      h1("Let's count!"),
      Counter.create("Sheep", initialStep = 3)
    )
    app.renderInto(node.asInstanceOf[fs2.dom.Node[IO]]).allocated
  }
}

private def calicoHelloWorld(client: Client[IO]) = {
  newNode[IO]("app", "calico-hello-world").flatMap { node =>
    val app = div(
      h1("Server demo!"),
      Hello.world(client)
    )
    app.renderInto(node.asInstanceOf[fs2.dom.Node[IO]]).allocated
  }
}

private def calicoNumbers(client: Client[IO]) = {
  newNode[IO]("app", "calico-number-stream").flatMap { node =>
    val app = div(
      h1("Streaming demo!"),
      Numbers.oneButtonParallelStreamer(client),
      Numbers.oneButtonSupervisedStreamer(client),
      Numbers.oneButtonHotswappedStreamer(client),
      Numbers.twoButtonStreamer(client),
    )
    app.renderInto(node.asInstanceOf[fs2.dom.Node[IO]]).allocated
  }
}

private def program: IO[Unit] =
  FetchClientBuilder[IO].resource.use { client =>
    Stream.eval(Queue.unbounded[IO, String]).flatMap { logs =>

      val program: Stream[IO, Unit] = Stream.exec {
        logs.offer("Hello, World") >>
          helloWorld.void >>
          createAppDiv.void >>
          calicoColours.void >>
          calicoCounter.void >>
          calicoHelloWorld(client).void >>
          calicoNumbers(client).void
      }

      val logging = Stream.fromQueueUnterminated(logs).debug(s => s"log: $s")

      Stream(
        program,
        logging // this never terminates
      )
        .parJoinUnbounded
    }
      .compile
      .drain
  }

@main def run(): Unit = program.unsafeRunAndForget()
