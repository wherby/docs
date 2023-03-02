package modules.forComprehension

object SerialFutureMap2 extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global

  def f1 = Future {
    Thread.sleep(300)
    println("[Serial] exec f1 first though it sleep some time.")
    "f1"
  }

  def f2 = Future {
    println("[Serial] exec f2 second.")
    "f2"
  }

  val parallel = for {
    first <- f1
    second <- f2

  } yield {
    first + " and " + second
  }

  parallel foreach {
    case result => println("Serial result:" + result)
  }

  Thread.sleep(1000)
}

