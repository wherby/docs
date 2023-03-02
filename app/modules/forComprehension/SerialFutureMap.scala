package modules.forComprehension
object SerialFutureMap extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  
  val serial = for {
    f1 <- Future {
      Thread.sleep(300)
      println("[Serial] exec f1 first.")
      "f1"
    }
    
    f2 <- Future {
      println("[Serial] exec f2 second.")
      "f2"
    }
  } yield {
    f1 + " and " + f2
  }
  
  serial foreach {
    case result => println("Serial result:" + result)
  }

  Thread.sleep(1000)
}

