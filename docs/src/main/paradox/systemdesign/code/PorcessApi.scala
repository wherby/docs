trait ProcessTranApi extends AskProcessResult{
  this: SystemApi with DriverApi =>
  val processTranActor = actorSystem.actorOf(ProcessTranActor.processTranActorProps, CNaming.timebasedName( "defaultProcessTranActor"))

  def runProcessCommand(processCallMsg: ProcessCallMsg, timeout: Timeout = longTimeout)(implicit ex: ExecutionContext): Future[JobResult] = {
    val processJob = JobMsg("SimpleProcess", processCallMsg)
    val receiveActor = actorSystem.actorOf(ReceiveActor.receiveActorProps, CNaming.timebasedName( "Receive"))
    val processJobRequest = JobRequest(processJob, receiveActor, processTranActor)
    getProcessCommandFutureResult(processJobRequest, defaultDriver, receiveActor,timeout)
  }
}