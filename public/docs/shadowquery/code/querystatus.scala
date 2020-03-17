  override def queryFundReportStatus(engagementId: String): Future[Seq[FundEngagementWithStatus]] = {
    val sql = for ((((fundEnagements, funds), selections), ega)
                     <- FundEngagement join Funds on ((left, right) => left.fundid === right.id && left.engagementid === engagementId)
                                       joinLeft  FundEngagementReportTypeSelection on ((left, right) => left._1.id === right.fundEngagementId && right.selected === true)
                                       joinLeft Ega on ((left, right) => left._1._1.id === right.fundEngagementId))
      yield {
        var isReportTypeDefined: Rep[Boolean]  = selections.isDefined
        var hasReportUploadedContent = selections.map(_.uploadFileStatus === UploadFileStatus.success.toString)
        var hasEgaDownloaded: Rep[Boolean] = ega.map(_.generateStatus === GenerateStatus.success.toString).getOrElse(false)
        (fundEnagements.id, fundEnagements.engagementid, fundEnagements.fundid, funds.name, isReportTypeDefined, hasReportUploadedContent, hasEgaDownloaded)
      }

    for {
      result <- db.run(sql.result).map(rows => rows.collect { case dataRow => (FundEngagementWithStatusBoolean.apply _).tupled(dataRow) })
    } yield {
      var sqlResult = result.map(item => {
        var definedReportTypeNumber = if(item.isReportTypeDefined) 1 else 0
        var reportUploadedNumber = if(item.hasReportUploadedPath == Some(true)) 1 else 0
        FundEngagementWithStatus(item.fundEngagementId,item.engagementId, item.fundId, item.fundName, definedReportTypeNumber, reportUploadedNumber, item.hasEgaDownloaded)
      })

      var combineStatus = Seq[FundEngagementWithStatus]()
      for(status <- sqlResult){
        if(combineStatus.find(_.fundEngagementId == status.fundEngagementId).isEmpty){ // has put into combineStatus sequence

          var statusWithSameFundEngagementId: Seq[FundEngagementWithStatus] = sqlResult.filter(_.fundEngagementId == status.fundEngagementId)

          var definedReportNumbers:Int = 0
          var uploadedReportNumbers:Int = 0
          var hasEgaDownloaded:Boolean = true
          for(fundStatus <- statusWithSameFundEngagementId){
            definedReportNumbers = definedReportNumbers + fundStatus.definedReportTypeNumber
            uploadedReportNumbers = uploadedReportNumbers + fundStatus.reportUploadedNumber
            hasEgaDownloaded = hasEgaDownloaded && fundStatus.hasEgaDownloaded
          }
          combineStatus = combineStatus :+ FundEngagementWithStatus(status.fundEngagementId, status.engagementId, status.fundId, status.fundName, definedReportNumbers, uploadedReportNumbers, hasEgaDownloaded)
        }
      }
      combineStatus
    }
  }
