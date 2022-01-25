import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher

trait Logger {
  def log(message: String): Unit
}

object Logger {
  def apply[F[_]](dispatcher: Dispatcher[F])(using F: Sync[F]): F[Logger] = {
    F.delay {
      (message: String) => dispatcher.unsafeRunAndForget(F.delay(println(message)))
    }
  }
}
