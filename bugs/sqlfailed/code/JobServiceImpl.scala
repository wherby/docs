class JobServiceImpl @Inject()(statDao: StatDAO) extends JobService {
  var state: JobServiceState = JobServiceState(None, false, false, LocalTime.now(), Some(LocalTime.now()), Nil)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  final val failureLimit = 5
  override def getState: JobServiceState =
    state

  override def updateRunning(running: Boolean): Future[JobServiceState] =
    Future {
      state = state.copy(running = running)
      state
    }

  override def updateStartTime(startTime: LocalTime): Future[JobServiceState] =
    Future {
      state = state.copy(startTime = startTime)
      state
    }

  override def updateEndTime(endTime: Option[LocalTime]): Future[JobServiceState] =
    Future {
      state = state.copy(endTime = endTime)
      state
    }

  override def updateSuccessful(successful: Boolean): Future[JobServiceState] =
    Future {
    state = state.copy(successful = successful)
    state
  }


  override def updateId(idOpt: Option[UUID]): Future[JobServiceState] =
    Future {
      state = state.copy(id = idOpt)
      state
    }

  override def startAJob(uuid: UUID): Future[Seq[Stat]] = {
    if (state.running) {
      LoggerFactory.getLogger(this.getClass).warn(s"${state.id} already running.")
      Future.failed(new RuntimeException("is running!!!"))
    } else {
      LoggerFactory.getLogger(this.getClass).info(s"start another job $uuid")
      val EngagementsStats = updateRunning(true)
        .flatMap(_ => updateId(Some(uuid)))
        .flatMap(_ => updateStartTime(LocalTime.now()))
        .flatMap(_ => updateEndTime(None))
        .flatMap(_ => statDao.compute)

      EngagementsStats.onComplete {
        case Success(value) =>
          LoggerFactory.getLogger(this.getClass).info(s"${state.id.getOrElse("null")} successfully complete, using time ${ChronoUnit.SECONDS.between(state.startTime, LocalTime.now())}")
          updateRunning(false)
            .flatMap(_ => updateSuccessful(true))
            .flatMap(_ => updateEndTime(Some(LocalTime.now())))
            .foreach(_ => {
              state = state.copy(history = state.copy(history = Nil) :: state.history)
            })
        case Failure(exception) =>
          LoggerFactory.getLogger(this.getClass).info(s"${state.id.getOrElse("null")} failed to complete")
          updateRunning(false)
            .flatMap(_ => updateSuccessful(false))
            .flatMap(_ => updateEndTime(Some(LocalTime.now())))
            .foreach(_ => {
              state = state.copy(history = state.copy(history = Nil) :: state.history)
            })
      }
      EngagementsStats
    }
  }

  override def startAJob(): Future[Seq[Stat]] = startAJob(UUID.randomUUID())
}