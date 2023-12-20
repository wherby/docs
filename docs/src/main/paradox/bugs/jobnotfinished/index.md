# Job not finished

## issue

When job does finish in good status, then there will have the dead letter as below:

``` log
2023-10-31 17:03:56,443 [INFO] from akka.actor.LocalActorRef in default-akka.actor.default-dispatcher-10 - akka://default/user/driver5feb3022-a5ca-4540-a324-7416f80c988a/SimpleProcessFuture9c1718d6-bb6f-4141-9c4c-90bca2d30e7a Message [scala.Enumeration$Val] from Actor[akka://default/user/driver5feb3022-a5ca-4540-a324-7416f80c988a/SimpleProcessFuture9c1718d6-bb6f-4141-9c4c-90bca2d30e7a#-2019376894] to Actor[akka://default/user/driver5feb3022-a5ca-4540-a324-7416f80c988a/SimpleProcessFuture9c1718d6-bb6f-4141-9c4c-90bca2d30e7a#-2019376894] was not delivered. [8] dead letters encountered. If this is not an expected behavior then Actor[akka://default/user/driver5feb3022-a5ca-4540-a324-7416f80c988a/SimpleProcessFuture9c1718d6-bb6f-4141-9c4c-90bca2d30e7a#-2019376894] may have terminated unexpectedly. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
```

And then all job will timeout, the dead letter will be:

``` log
2023-10-31 19:32:35,719 [INFO] from akka.actor.LocalActorRef in default-akka.actor.default-dispatcher-11 - akka://default/user/driver5feb3022-a5ca-4540-a324-7416f80c988a/SimpleProcessFuturef84aa674-5b1f-4962-a3d8-1091385a152d Message [doracore.core.msg.Job$JobResult] from Actor[akka://default/user/driver5feb3022-a5ca-4540-a324-7416f80c988a/queueActorc6d42113-fdb3-46ee-a19b-4c81a08ac337#928142608] to Actor[akka://default/user/driver5feb3022-a5ca-4540-a324-7416f80c988a/SimpleProcessFuturef84aa674-5b1f-4962-a3d8-1091385a152d#1446614358] was not delivered. [9] dead letters encountered. If this is not an expected behavior then Actor[akka://default/user/driver5feb3022-a5ca-4540-a324-7416f80c988a/SimpleProcessFuturef84aa674-5b1f-4962-a3d8-1091385a152d#1446614358] may have terminated unexpectedly. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
```

And we find the job is started

``` log
2023-10-31 17:03:41,923 [INFO] from doracore.core.fsm.FsmActor in default-akka.actor.default-dispatcher-13 - akka://default/user/driver5feb3022-a5ca-4540-a324-7416f80c988a/fsmActorbb55be19-7f74-4fbc-b923-70746ba7d29c {Some(JobMeta(TableDetectionProcessorPROJ_11326_slowJob_1698771754363))} is started in fsm worker, and will be handled by {Actor[akka://default/user/defaultProcessTranActordb78414b-eb41-4553-90a3-5d14608e581f#252787697]}
```

while there is job end message

## [Fix](https://github.com/wherby/dora/commit/0e673e43fbec66f8ce1ddaadf892646a38c628ac) 

The PoisonPill will kill the actor immediately, when the actor is killed, can't reset the fsm to take another job.

``` scala 
  def getResult(receiveActor: ActorRef, timeout: Timeout): Future[JobResult] = {
    implicit val ex: ExecutionContext = getBlockDispatcher()
    implicit val timeoutValue: Timeout = timeout
    var result = JobResult(JobStatus.Unknown, "Unkonwn").asInstanceOf[Any]
    (receiveActor ? FetchResult())
      .map { resultT =>
        resultT.asInstanceOf[JobResult]
      }
      .recover {
        case ex: Throwable =>
          val tName = Thread.currentThread.getName
          Logger
            .apply(this.getClass.getName)
            .error(s"$tName=> Job timeout after $timeout")
          result =
            JobResult(JobStatus.TimeOut, ProcessResult(JobStatus.Failed, ex))
          receiveActor ! ProxyControlMsg(JobStatus.Canceled)
          //receiveActor ! PoisonPill
          result.asInstanceOf[JobResult]
      }
      .map { result =>
        receiveActor ! ProxyControlMsg(PoisonPill)
        receiveActor ! PoisonPill
        result
      }
```

