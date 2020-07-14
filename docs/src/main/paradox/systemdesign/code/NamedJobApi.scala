trait NamedJobRunner {
  this: BackendServer.type =>

  def runNamedProcessCommand(processJob: JobMsg,
                             jobName:String,
                             timeout: Timeout = ConstVars.longTimeOut,
                             priority: Option[Int] = None)(implicit ex: ExecutionContext): Future[JobResult] = {
    val jobApi = getNamedJobApi(jobName)
    val receiveActor = jobApi.actorSystem.actorOf(ReceiveActor.receiveActorProps, CNaming.timebasedName("Receive"))
    val processJobRequest = JobRequest(processJob, receiveActor, jobApi.processTranActor, priority)
    getProcessCommandFutureResult(processJobRequest, jobApi.defaultDriver, receiveActor,timeout)
  }
}