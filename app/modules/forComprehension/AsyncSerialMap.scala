package modules.forComprehension

object AsyncSerialMap extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  import scala.async.Async.{async, await}

  def f1(name : String) = Future {
    Thread.sleep(300)
    println("[Serial] exec f1 first though it sleep some time.")
    name
  }

  def f2(name : String) = Future {
    println("[Serial] exec f2 second.")
    name
  }

  val serial = async {
    await(f1("f1")) + " and " + await(f2("f2"))
  }

  serial foreach {
    case result => println("serial result:" + result)
  }
  Thread.sleep(1000)
}



