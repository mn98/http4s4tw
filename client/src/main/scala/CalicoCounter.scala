import calico.dsl.io.*
import calico.syntax.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.*
import org.scalajs.dom.HTMLElement

object CalicoCounter {

  def create(label: String, initialStep: Int): Resource[IO, HTMLElement] =
    SignallingRef[IO].of(initialStep).product(Channel.unbounded[IO, Int])
      .toResource.flatMap { (step, diff) =>

      val allowedSteps = List(1, 2, 3, 5, 10)

      div(
        p(
          "Step: ",
          select(
            allowedSteps.map(step => option(value := step.toString, step.toString)),
            value <-- step.map(_.toString),
            onChange --> {
              _.mapToTargetValue.map(_.toIntOption).unNone.foreach(step.set)
            }
          )
        ),
        p(
          label + ": ",
          b(diff.stream.scanMonoid.map(_.toString)),
          " ",
          button(
            "-",
            onClick --> {
              _.evalMap(_ => step.get).map(-1 * _).foreach(diff.send(_).void)
            }
          ),
          button(
            "+",
            onClick --> {
              _.evalMap(_ => step.get).foreach(diff.send(_).void)
            }
          )
        )
      )
    }

}