package modules.forComprehension

object ParallelFutureMap4 extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global

  val f1 = Future {
    Thread.sleep(2000)
    println("[Parallel] exec f1 second as it is sleep some time.")
    "f1"
  }

  val f2 = Future {
    Thread.sleep(100)
    println("[Parallel] exec f2 first.")
    "f2"
  }

  val parallel = for {
    first <- f1
    f3<-Future{println("Print in for")
      "f3"
    }
    second <- f2
  } yield {
    f3 + " and "+first + " and " + second
  }

  parallel foreach {
    case result => println("Parallel result:" + result)
  }
  Thread.sleep(4000)
}

/*
[Parallel] exec f2 first.
[Parallel] exec f1 second as it is sleep some time.
Print in for
Parallel result:f3 and f1 and f2
 */