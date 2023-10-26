# Cancel task issue


## 

From log, there is only task start log :

``` log
02:42:57.590 [default-akka.actor.default-dispatcher-6] [[37minfo[0m] d.c.f.FsmActor akka://default/user/driver7c6baa49-ab9a-406c-80a1-f620e7732fee/fsmActor782c0df9-b3f8-4948-a294-5ef39dadb69c - {Some(JobMeta(TableDetectionProcessor))} is started in fsm worker, and will be handled by {Actor[akka://default/user/defaultProcessTranActor0302425b-0f73-4bff-a9c8-4ebd1815a9bf#-1692441311]}


```

we can't find task end log like

``` log
06:07:39.059 [default-akka.actor.default-dispatcher-18] [[37minfo[0m] d.c.f.FsmActor akka://default/user/driver7c6baa49-ab9a-406c-80a1-f620e7732fee/fsmActor782c0df9-b3f8-4948-a294-5ef39dadb69c - Some(JobMeta(TableDetectionProcessor)) is end
``` 

Which means the task is send to driver and not start or end in normal way as:

``` scala
  when(Active) {
    case Event(jobEnd: JobEnd, task: Task) =>
      if (jobEnd.requestMsg.jobMetaOpt == jobMetaOpt) {
        log.log(Logging.InfoLevel,s"$jobMetaOpt is end")
        goto(Idle) using (Uninitialized)
      } else {
        stay()
      }
```

The issue happend after timeout 

``` log
05:13:16.240 [default-akka.actor.default-dispatcher-8] [[31merror[0m] d.b.BackendServer$  - default-akka.actor.default-dispatcher-8=> Job timeout after Timeout(9000 seconds)
05:13:16.244 [default-akka.actor.default-dispatcher-21] [[37minfo[0m] a.a.DeadLetterActorRef akka://default/deadLetters - Message [doracore.core.msg.Job$JobResult] from Actor[akka://default/user/Receive926e10d4-f58d-4f24-8ae4-4f58d4397322#-1243994010] to Actor[akka://default/deadLetters] was not delivered. [3] dead letters encountered. If this is not an expected behavior then Actor[akka://default/deadLetters] may have terminated unexpectedly. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
05:13:16.341 [processor-pool-thread--1] [[31merror[0m] c.p.d.c.e.c.JobInstance  - processor failed, akka.pattern.AskTimeoutException: Ask timed out on [Actor[akka://default/user/Receive926e10d4-f58d-4f24-8ae4-4f58d4397322#-1243994010]] after [9000000 ms]. Message of type [doracore.tool.receive.ReceiveActor$FetchResult]. A typical reason for `AskTimeoutException` is that the recipient actor didn't send a reply.
```

There is [a bug](https://github.com/wherby/dora/blob/35033b3ce1c6ca4a36eb30ccc38259e462792571/dora/src/main/scala/doracore/api/AskProcessResult.scala) handling time out:

```scala
  def getResult(receiveActor: ActorRef, timeout: Timeout): Future[JobResult] = {
    implicit val ex: ExecutionContext  = getBlockDispatcher()
    implicit val timeoutValue: Timeout = timeout
    var result                         = JobResult(JobStatus.Unknown, "Unkonwn").asInstanceOf[Any]
    (receiveActor ? FetchResult())
      .map { resultT =>
        resultT.asInstanceOf[JobResult]
      }
      .recover { case ex: Throwable =>
        val tName = Thread.currentThread.getName
        Logger.apply(this.getClass.getName).error(s"$tName=> Job timeout after $timeout")
        result = JobResult(JobStatus.TimeOut, ProcessResult(JobStatus.Failed, ex))
        receiveActor ! ProxyControlMsg(result)
        Thread.sleep(100)
        result.asInstanceOf[JobResult]
      }
      .map { result =>
        receiveActor ! ProxyControlMsg(PoisonPill)
        receiveActor ! PoisonPill
        result
      }
  }
```

when Job  timeout, the timeout result will send to proxyActor to stop FSM, but if the job not send to FSM, the Result message will not remove the task and may remove another
samename(by jobmeta) task.


```scala
  def finishTask() = {
    fsmActorOpt.map { fsmActor =>
      fsmActor ! JobEnd(requestMsgBk)
    }
  }

  override def receive: Receive = {
    case jobRequest: JobRequest =>
      handleJobRequest(jobRequest)
    case jobResult: JobResult =>
      result = Some(jobResult.result)
      replyTo ! jobResult
      self ! JobStatus.Finished
    case JobStatus.Scheduled =>
      fsmActorOpt = Some(sender())
      status = JobStatus.Scheduled
    case JobStatus.Finished | JobStatus.Failed | JobStatus.TimeOut =>
      finishTask()
```

if the timeout job remove another job, which will let another job stop silently, and keep client waiting for result.


The below is a test case, job1 is a blocking job which will cause timeout, job2 is a regular job, if we remove all "Thread.sleep(3000)", the result will be timeout for job2.
Because, multiple job1 will timeout at same time, and only one job which is in fsm will be removed, after job1 timeout, there will still have job1 runing in fsm, job2 can't
be executed.

```scala
    "Name Job with Meta" must {
      "run job in sequece the sleep operation will not block following operation and time out will go" in {
        val job1 = TestVars.sleepProcessJob
        BackendServer.runNamedProcessCommand(
          job1,
          "job13",
          metaOpt = Some(JobMeta("NewNameJob1")),
          timeout = ConstVars.timeout1S
        )
        //BackendServer.runNamedProcessCommand(job1, "job13",metaOpt = Some(JobMeta("NewNameJob1")),timeout=ConstVars.timeout1S  )
        //BackendServer.runNamedProcessCommand(job1, "job13",metaOpt = Some(JobMeta("NewNameJob1")),timeout=ConstVars.timeout1S  )
        Thread.sleep(3000)
        val job2 = TestVars.processJob
        BackendServer.runNamedProcessCommand(
          job2,
          "job13",
          metaOpt = Some(JobMeta("NewNameJob2"))
        )
        //Thread.sleep(3000)
        Thread.sleep(3000)
        BackendServer.runNamedProcessCommand(
          job1,
          "job13",
          metaOpt = Some(JobMeta("NewNameJob1")),
          timeout = ConstVars.timeout1S
        )
        Thread.sleep(3000)
        BackendServer.runNamedProcessCommand(
          job1,
          "job13",
          metaOpt = Some(JobMeta("NewNameJob1")),
          timeout = ConstVars.timeout1S
        )
        Thread.sleep(3000)
        BackendServer.runNamedProcessCommand(
          job1,
          "job13",
          metaOpt = Some(JobMeta("NewNameJob1")),
          timeout = ConstVars.timeout1S
        )
        Thread.sleep(3000)
        BackendServer.runNamedProcessCommand(
          job1,
          "job13",
          metaOpt = Some(JobMeta("NewNameJob1")),
          timeout = ConstVars.timeout1S
        )
        Thread.sleep(3000)
        BackendServer.runNamedProcessCommand(
          job1,
          "job13",
          metaOpt = Some(JobMeta("NewNameJob1")),
          timeout = ConstVars.timeout1S
        )
        Thread.sleep(3000)
        val resultFuture = BackendServer.runNamedProcessCommand(
          job2,
          "job13",
          metaOpt = Some(JobMeta("NewNameJob2"))
        )
        val result =
          try {
            Await.ready(resultFuture, timeout)
          } catch {
            case _: Throwable => Future("TimeOutError")
          }

        println("s")
        println(result)
        println("XX")

        Thread.sleep(2000)
      }
    }
```

If we add sleep in each timeout, and make sure cancelled job in fsm, then the job2 will be success finished.


## Remove the cancel job in fsm and queue

[The fix](https://github.com/wherby/dora/commit/8efb35bde986496911b462b86a543e71ab0ad7e3) to issue is remove the cancelled task both in queue and fsm.

```scala
 
dora/src/main/scala/doracore/api/AskProcessResult.scala
...
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

dora/src/main/scala/doracore/core/queue/QueueActor.scala
...
  def handleRemove(jobRequest: JobRequest) = {
    log.info(s"Job: $jobRequest  which is canceled by user.")
    val eleToRemove = taskQueue.snap().filter(_.jobMetaOpt ==jobRequest.jobMetaOpt)
    eleToRemove.headOption match {
      case Some(eleFind)=>
        val removedJob = taskQueue.removeEle(eleFind)
        log.info(s"there is a job to be removed $removedJob")
        removedJob.map { job =>
          job.replyTo ! JobResult(JobStatus.Canceled, s"Job: $job  which is canceled by user.")
        }
      case _=>



dora/src/main/scala/doracore/core/proxy/ProxyActor.scala
...

  def cancelJob()={
      queueActor ! RemoveJob(requestMsgBk)
      fsmActorOpt.map { fsmActor =>
        fsmActor ! JobEnd(requestMsgBk)
      }
  }

  override def receive: Receive = {
    case jobRequest: JobRequest =>
      handleJobRequest(jobRequest)
  class ProxyActor(queueActor: ActorRef) extends BaseActor {
      status = JobStatus.Scheduled
    case JobStatus.Finished | JobStatus.Failed | JobStatus.TimeOut =>
      finishTask()
    case JobStatus.Canceled =>
      cancelJob()


```