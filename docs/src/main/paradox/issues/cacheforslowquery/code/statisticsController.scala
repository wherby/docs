  def lookupEngagementsCompletion() = deadbolt.SubjectPresent()(parse.anyContent) {
    implicit authRequest =>
      {
        val engagementIdsJsArray = authRequest.body.asJson
        val format               = new SimpleDateFormat("d-M-y")
        val date                 = format.format(Calendar.getInstance().getTime())
        engagementIdsJsArray match {
          case Some(array) => {
            val engagementIds   = array.as[Seq[String]]
            val engagementHash  = engagementIds.mkString(",").hashCode
            val dateId          = date + engagementHash.toString
            val resultOptFuture = sysCacheService.lookupStatistLogRecord(dateId)
            resultOptFuture.flatMap { resultOpt =>
              resultOpt match {
                case Some(result) =>
                  val resultCache = result.cache
                  val staticsLog = Json
                    .parse(resultCache)
                    .asOpt[CacheRecordInSys]
                    .flatMap { entity =>
                      entity.staticsLog
                    }
                    .getOrElse("{}")
                  println("get from cache")
                  Future(Ok(staticsLog))
                case _ =>
                  sysCacheService.updateStaticsLog("{}", dateId).flatMap { _ =>
                    getCompletionRatioByEngagementIds(engagementIds).map(res => {
                      sysCacheService.updateStaticsLog(Json.toJson(res).toString(), dateId)
                      println(s"Calc the value for $dateId")
                      Ok(Json.toJson(res))
                    })
                  }
              }
            }
          }
          case _ => {
            val emptyMap: Map[String, Double] = Map()
            Future(Ok(Json.toJson(emptyMap)))
          }
        }
      }
  }