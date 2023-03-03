package modules.forComprehension


object ParallelFutureMap2 extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global

  val f1 = Future {
    Thread.sleep(1000)
    println("[Parallel] exec f1 second as it is sleep some time.")
    "f1"
  }

  val f2 = Future {
    Thread.sleep(500)
    println("[Parallel] exec f2 first.")
    "f2"
  }

  val parallel = for {
    first <- f1
    _<-Future{println("Print in for")
      "f3"
    }
    second <- f2
  } yield {
    first + " and " + second
  }

  parallel foreach {
    case result => println("Parallel result:" + result)
  }
  Thread.sleep(1000)
}

/*
[Parallel] exec f2 first.
[Parallel] exec f1 second as it is sleep some time.
Print in for
Parallel result:f1 and f2
 */