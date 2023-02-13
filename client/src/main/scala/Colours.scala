import calico.*
import calico.html.io.{*, given}
import calico.syntax.*
import calico.unsafe.given
import cats.effect.*
import cats.effect.std.Random
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.*
import fs2.dom.HtmlDivElement
import org.scalajs.dom

object Colours {

  private val default: String = s"rgb(255,255,255)"

  def create: Resource[IO, HtmlDivElement[IO]] =
    SignallingRef[IO].of(default).product(Random.scalaUtilRandom).toResource.flatMap { (colour, rng) =>

      def setColour(c: String) = colour.set(c) >> IO(dom.document.body.style.backgroundColor = c)

      div(
        button(
          "Change",
          onClick --> {
            _.foreach { _ =>
              for {
                r <- rng.nextIntBounded(256)
                g <- rng.nextIntBounded(256)
                b <- rng.nextIntBounded(256)
                _ <- setColour(s"rgb($r,$g,$b)")
              } yield ()
            }
          }
        ),
        button(
          "Reset",
          onClick --> {
            _.foreach { _ => setColour(default) }
          }
        ),
        label(colour.discrete.map(c => s"Background colour is set to $c").holdOptionResource)
      )
    }

}