 private def cleanSelectionType(oldFund:Fund, newFund: Fund, fundenegagementId:String ):Future[Any]={
    if(oldFund.fundAdmin != newFund.fundAdmin){
      fundEngagementReportTypeSelectionDAO.deleteByEngagementId(fundenegagementId)
    }
    else if(!oldFund.auditPeriodBegin.equals(newFund.auditPeriodBegin) ||
      !oldFund.auditPeriodEnd.equals(newFund.auditPeriodEnd) ||
     !oldFund.baseCurrency.equals(newFund.baseCurrency)){
      fundEngagementReportTypeSelectionWrite.clearUploadedReports(fundenegagementId)
      egaWrite.cleanEGAByFundEngagementId(fundenegagementId)
    }else {
      Future(1)
    }
  }

  override def update(fundEngagement: FundEngagementData): Future[Int] = {
    val newFundOpt = fundEngagement.fundRecord.map{
      record=>Json.parse(record).as[Fund]
    }
    val oldFunFutureOpt = fundEngagementDAO.lookup(fundEngagement.id).map{
      fundEngagementOpt => fundEngagementOpt.flatMap{
        fundengagementTemp=>fundengagementTemp.fundRecord.map{
          record=>Json.parse(record).as[Fund]
        }
      }
    }
    val cleanUpTask= oldFunFutureOpt.flatMap{
      oldFunOpt=> oldFunOpt.flatMap{
        oldFun=> newFundOpt.map{
          newFund => cleanSelectionType(oldFun, newFund,fundEngagement.id)
        }
      }match {
        case Some(futureThing)=> futureThing
        case _=>Future(1)
      }
    }
    cleanUpTask.flatMap { result =>
      val newFundTemp = newFundOpt.get
      val newFundFuture =  fundAdminDAO.queryByFundAdmin(newFundTemp.fundAdmin).map{
        tmpOpt =>tmpOpt match{
          case Some(fundAdmin)=>newFundTemp.copy(fundAdminId = Some(fundAdmin.id))
          case _=>newFundTemp
        }
      }
      val newFund = Await.result(newFundFuture, 1 second)
      val newFundEngagement = fundEngagement.copy(fundRecord = Some(Json.toJson(newFund).toString()))
      fundEngagementDAO.update(newFundEngagement)
    }
  }