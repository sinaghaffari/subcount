package utils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by Sina Ghaffari on 2016-09-05.
  */
object FutureFailIf {
  class FutureFailIf[T](future: Future[T]) {
    def failIf[S](s: (T) => Boolean)(f: (T) => Exception) = {
      future.flatMap { t =>
        s(t) match {
          case false => Future.successful(t)
          case true => Future.failed(f(t))
        }
      }
    }
  }

  implicit def futureFailIf[T](future: Future[T]): FutureFailIf[T] = new FutureFailIf[T](future)
}
