import cats.effect.kernel.Sync
import cats.effect.{IO, IOApp}
import cats.syntax.all._
import org.scalajs.dom
import slinky.core.facade.ReactElement
import slinky.web

object Main extends IOApp.Simple {

  def render[F[_]](
                    parentName: String,
                    childName: String,
                    child: ReactElement,
                    log: String => F[Unit],
                    additional: Option[F[Unit]] = None,
                  )(
                    implicit F: Sync[F],
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
            additional.traverse_(_.void) >>
            log(s"Rendered $parentName -> $childName")
        }
          .whenA(parent != null)
      }
  }

  val logger: String => IO[Unit] = s => IO(println(s))

  override def run: IO[Unit] = {
    IO(println(s"Hello, World")) >>
      IO({
        val parNode = dom.document.createElement("p")
        val textNode = dom.document.createTextNode("Hello, World")
        parNode.appendChild(textNode)
        dom.document.body.appendChild(parNode)
      }) >>
      IO({
        val appDiv = dom.document.createElement("div")
        appDiv.id = "app"
        dom.document.body.appendChild(appDiv)
      }) >>
      render("app", "click-counter", ClickCounter.component(()), logger)
  }
}
