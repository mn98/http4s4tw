import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import org.scalajs.dom

object Main extends IOApp.Simple {

  val logger: String => IO[Unit] = s => IO(println(s))

  override def run: IO[Unit] = {
    IO(println(s"Hello, World")) >>
      IO({
        val parNode = dom.document.createElement("p")
        val textNode = dom.document.createTextNode("Hello, World")
        parNode.appendChild(textNode)
        dom.document.body.appendChild(parNode)
      })
  }
}
