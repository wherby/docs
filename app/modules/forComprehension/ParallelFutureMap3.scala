package modules.forComprehension

object ParallelFutureMap3 extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global

  val f1 = Future {
    Thread.sleep(3000)
    println("[Parallel] exec f1 second as it is sleep some time.")
    "f1"
  }

  val f2 = Future {
    Thread.sleep(1000)
    println("[Parallel] exec f2 first.")
    "f2"
  }
  val f3f = Future{println("Print in for")
    "f3"
  }

  val parallel = for {
    first <- f1
    f3<-f3f
    second <- f2
  } yield {
    f3+" and "+ first + " and " + second
  }

  parallel foreach {
    case result => println("Parallel result:" + result)
  }
  Thread.sleep(4000)
}

/*
Print in for
[Parallel] exec f2 first.
[Parallel] exec f1 second as it is sleep some time.
Parallel result:f3 and f1 and f2
 */