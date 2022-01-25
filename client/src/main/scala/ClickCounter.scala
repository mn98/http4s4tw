import org.scalajs.dom
import slinky.core.FunctionalComponent
import slinky.core.facade.Hooks.{useEffect, useState}
import slinky.core.facade.ReactElement
import slinky.web.html.*

object ClickCounter {

  type Props = Unit

  def apply(): ReactElement = component(())

  private val random = new scala.util.Random()

  private val component: FunctionalComponent[Props] = FunctionalComponent[Props] { _ =>

    val (count, setCount) = useState(0)

    useEffect(
      () => {
        if (count > 0) {
          val r = random.nextInt(256)
          val g = random.nextInt(256)
          val b = random.nextInt(256)
          dom.document.body.style.backgroundColor = s"rgb($r,$g,$b)"
        } else {
          dom.document.body.style.backgroundColor = s"rgb(255,255,255)"
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
