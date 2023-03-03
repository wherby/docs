package modules.forComprehension

object ParallelFutureMap5 extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global

  val f1 = Future {
    Thread.sleep(5000)
    println("[Parallel] exec f1 second as it is sleep some time.")
    "f1"
  }

  val f2 = Future {
    Thread.sleep(1000)
    println("[Parallel] exec f2 first.")
    "f2"
  }
  val f3 = Future{println("Print in for")
    "f3"
  }

  val parallel = for {
    first <- f1
    third<-f3
    second <- f2
  } yield {
     first + " and "+ third+" and " + second
  }

  parallel foreach {
    case result => println("Parallel result:" + result)
  }
  Thread.sleep(6000)
}

/*
Print in for
[Parallel] exec f2 first.
[Parallel] exec f1 second as it is sleep some time.
Parallel result:f1 and f3 and f2
 */