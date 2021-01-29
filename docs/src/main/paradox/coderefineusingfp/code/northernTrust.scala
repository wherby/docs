object GuessNorthernTrust {

  val isNorthernTrust = (name:String)=>{
    belongToSomeSelectedFunds(name,northernTrust)
  }

  def generate(cvg:ConfirmationValuationGSP,selections:Seq[FundEngagementReportTypeSelectionData]) = {
    val afterGuess = cvg.copy(workCash = guessWorkCash(cvg.workCash,selections))
    afterGuess
  }

  def guessWorkCash(workCash:Seq[Map[String,String]],selections:Seq[FundEngagementReportTypeSelectionData]) = {
    workCash.map(workCashRow=>{
      var glAccountName = workCashRow.get(WorkCashColumns.GL_ACCOUNT_NAME).getOrElse("")
      if(isNorthernTrust(glAccountName)){
        val IandU = guessingWorkCashIAndU(workCashRow,selections)
        workCashRow ++ Map(
          WorkCashColumns.BANK_ACCOUNT_NUMBER -> IandU._1,
          WorkCashColumns.AMOUNT_PER_CONFIRMATION -> IandU._2,
          WorkCashColumns.QUESTIONARE_FILE -> IandU._3
        )
      }else{
        workCashRow
      }
    })
  }

  /**
   *
   * @param workCashRow
   * @param selections
   * @return (ColumnI,ColumnU,fileName)
   */
  def guessingWorkCashIAndU(workCashRow:Map[String, String],selections:Seq[FundEngagementReportTypeSelectionData]) = {

    val workCashInfos = generateWorkCashInfos(workCashRow)
    var columns:Option[(String,String,String)] =  None

    var glAccountName = workCashRow.get(WorkCashColumns.GL_ACCOUNT_NAME).getOrElse("")
    if (isNorthernTrust(glAccountName)) {
      workCashInfos.foreach(workCashInfo => {
        if(columns.isEmpty){
          columns = getWorkCashColumnsFromReport(workCashInfo.reportId,workCashInfo.filterFunc,workCashInfo.getFunc,selections)
        }
      })
    }

    columns match {
      case Some(value) => value
      case None => (missingStr(glAccountName),missingStr(glAccountName),EMPTY_STR)
    }
  }

  def generateWorkCashInfos(workCashRow:Map[String, String]) = {
    val currency = workCashRow.getOrElse(WorkCashColumns.BANK_ACCOUNT_CURRENCY, "")

    val reportId = "5001"
    val filterFunc = (ob: JSONObject) => ob.getString("CURRENCY").trim == currency.trim
    val getFunc = (ob: JSONObject) => (ob.getString("ACCOUNT NUMBER") , ob.getString("BALANCE"))

    Seq(WorkCashInfo(reportId, filterFunc, getFunc))
  }

  def getWorkCashColumnsFromReport(reportID:String, filterFunc: JSONObject =>Boolean, getFunc: JSONObject =>(String,String), selections:Seq[FundEngagementReportTypeSelectionData]):Option[(String,String,String)] = {
    val report = selections.find(select => {
      select.sheettypesReporttypesMapId == reportID && select.extractionFileContent.isDefined
    })
    var result:Option[(String,String,String)] = None
    if(report.isDefined && result.isEmpty){
      val a = new JSONArray(report.get.extractionFileContent.get)
      for(i1 <- 0 until a.length()){
        var balance = ""
        var accNum = ""
        var keep = false
        val singleFileContent = a.get(i1).asInstanceOf[JSONObject]
        val fileNames = singleFileContent.names()
        for(i2 <- 0 until fileNames.length()){
          val fileName = fileNames.get(i2).toString
          val fileContents = singleFileContent.getJSONArray(fileName)
          for(i3 <- 0 until fileContents.length){
            val item = fileContents.getJSONObject(i3)
            item.names().get(0) match {
              case "ACCOUNT NUMBER" =>
                accNum = item.getString("ACCOUNT NUMBER")
              case "BALANCE" =>
                balance = item.getString("BALANCE")
              case "CURRENCY" =>
                if(filterFunc(item)){
                  keep = true
                }
              case _ =>
            }
          }
          if(keep)
            return Some(accNum, balance, fileName.split("\\|")(0))
        }
      }
    }
    result
  }

  case class WorkCashInfo(reportId:String, filterFunc: JSONObject =>Boolean, getFunc: JSONObject =>(String,String))
}