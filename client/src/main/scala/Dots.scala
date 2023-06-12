import cats.effect.IO
import cats.effect.std.Random
import cats.syntax.all.*
import fs2.Stream
import fs2.dom.HtmlCanvasElement
import org.scalajs.dom
import org.scalajs.dom.CanvasRenderingContext2D

import scala.concurrent.duration.DurationInt

object Dots {

  def draw(canvas: HtmlCanvasElement[IO]): IO[Unit] =
    Stream.eval(Random.scalaUtilRandom[IO]).flatMap { rng =>
      Stream.eval(IO {
        val jscanvas = canvas.asInstanceOf[dom.HTMLCanvasElement]
        val context: CanvasRenderingContext2D =
          jscanvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
        (jscanvas.width, jscanvas.height, context)
      }).flatMap { (w, h, context) =>
        Stream
          .eval((
            rng.betweenInt(0, w),
            rng.betweenInt(0, h),
            rng.betweenInt(2, 20),
            rng.betweenInt(0, 255),
            rng.betweenInt(0, 255),
            rng.betweenInt(0, 255),
          ).tupled)
          .map { (x, y, d, r, g, b) =>
            val colour = s"rgb($r, $g, $b)"
            context.clearRect(0, 0, w, h)
            context.beginPath()
            context.strokeStyle = colour
            context.arc(x, y, d, 0, math.Pi * 2, false)
            context.fillStyle = colour
            context.fill()
          }
          .repeat
          .meteredStartImmediately(100.millis)
      }
    }
      .compile
      .drain
}
