import org.scalajs.dom
import slinky.core.FunctionalComponent
import slinky.core.facade.Hooks.{useEffect, useState}
import slinky.core.facade.ReactElement
import slinky.web.html.*

object ClickCounter {

  case class Props(logger: Logger)

  def apply(logger: Logger): ReactElement = component(Props(logger))

  private val random = new scala.util.Random()

  private val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>

    val (count, setCount) = useState(0)

    useEffect(
      () => {
        if (count > 0) {
          val r = random.nextInt(256)
          val g = random.nextInt(256)
          val b = random.nextInt(256)
          val colour = s"rgb($r,$g,$b)"
          dom.document.body.style.backgroundColor = colour
          props.logger.log(s"Colour changed to $colour")
        } else {
          dom.document.body.style.backgroundColor = s"rgb(255,255,255)"
          props.logger.log(s"Colour reset to white")
        }
      },
      Seq(count)
    )

    div(
      p(s"You clicked $count times"),
      button(
        "Count",
        onClick := { () => setCount(count + 1) }
      ),
      button(
        "Reset",
        onClick := { () => setCount(0) }
      )
    )
  }

}
