package modules.forComprehension

object AsyncParallelMap extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  import scala.async.Async.{async, await}

  def f1(name: String) = Future {
    Thread.sleep(300)
    println("[parallel] exec f1 second as it sleep some time.")
    name
  }

  def f2(name: String) = Future {
    println("[parallel] exec f2 second.")
    name
  }

  val parallel = async {
    val result1 = f1("f1")
    val result2 = f2("f2")
    await(result1) + " and " + await(result2)
  }

  parallel foreach {
    case result => println("parallel result:" + result)
  }
  Thread.sleep(2000)
}

/*
[parallel] exec f2 second.
[parallel] exec f1 second as it sleep some time.
parallel result:f1 and f2
 */