object GuessHSBC {

  val thisfund = hsbc

  def belongToThisFund(name:String) = {
    belongToSomeSelectedFunds(name,thisfund)
  }

  def generate(cvg:ConfirmationValuationGSP,selections:Seq[FundEngagementReportTypeSelectionData]) = {
    val afterGuess = cvg.copy(workCash = guessWorkCash(cvg.workCash,selections),workInv = guessWorkInv(cvg.workInv,selections))
    afterGuess
  }

  def guessWorkCash(workCash:Seq[Map[String,String]],selections:Seq[FundEngagementReportTypeSelectionData]) = {
    workCash.map(workCashRow=>{
      var glAccountName = workCashRow.get(WorkCashColumns.GL_ACCOUNT_NAME).getOrElse("")
      if(belongToThisFund(glAccountName)){
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

  def guessWorkInv(workInv:Seq[Map[String,String]],selections:Seq[FundEngagementReportTypeSelectionData]) = {
    workInv.map(workInvRow=>{
      val custodian = workInvRow.get(WorkInvColumns.CUSTODIAN).getOrElse("")
      if(belongToThisFund(custodian)){
        val IandU = guessingWorkInvJandP(workInvRow,selections)
        workInvRow ++ Map(
          WorkInvColumns.INVESTMENT_VALUE_PER_CONFIRMATION -> IandU._1,
          WorkInvColumns.QUANTITY_PER_CONFIRMATION -> IandU._2,
          WorkInvColumns.QUESTIONARE_FILE -> IandU._3
        )
      }else{
        workInvRow
      }
    })
  }

  def getFirstMappingInConfirmationReport(reportID:String,filterFunc:(JSONObject)=>Boolean,selections:Seq[FundEngagementReportTypeSelectionData]):Map[String,JSONObject] = {
    val report = selections.filter(select=>{select.sheettypesReporttypesMapId == reportID && select.extractionFileContent.isDefined}).headOption
    var result:Map[String,JSONObject] = Map()
    if(report.isDefined && result.isEmpty){
      val a = new JSONArray(report.get.extractionFileContent.get)
      for(i1 <- a.length()-1 to 0 by -1){
        val singleFileContent = a.get(i1).asInstanceOf[JSONObject]
        val fileNames = singleFileContent.names()
        for(i2 <- 0 until fileNames.length()){
          val fileName = fileNames.get(i2).toString()
          val fileContents = new JSONArray(singleFileContent.getString(fileName))
          for(i3 <- 0 until fileContents.length){
            val item = fileContents.get(i3).asInstanceOf[JSONObject]
            if(filterFunc(item)){
              result  = result + (fileName.split("\\|")(0) -> item)
            }
          }
        }
      }
    }
    result
  }

  /**
   *
   * @param reportID
   * @param filterFunc
   * @param getFunc
   * @return (column I, column U ,fileName)
   */
  def getWorkCashColumnsFromReport(reportID:String,filterFunc:(JSONObject)=>Boolean,getFunc:(JSONObject)=>(String,String),selections:Seq[FundEngagementReportTypeSelectionData]):Option[(String,String,String)] = {
    val result = getFirstMappingInConfirmationReport(reportID,filterFunc,selections)
    if(result.isEmpty){
      None
    }else{
      var firstKey:Option[String] = None
      var IandU = ("","")
      for(k <- result.keys){
        if(firstKey.isEmpty){
          firstKey = Some(k)
          IandU = getFunc(result.get(k).get)
        }
      }
      Some(IandU._1,IandU._2,firstKey.get)
    }
  }


  /**
   *
   * @param reportID
   * @param filterFunc
   * @param getFunc
   * @return (column J, column P,fileName)
   */
  def getWorkInvColumnsFromReport(reportID:String,filterFunc:(JSONObject)=>Boolean,getFunc:(JSONObject)=>(String,String),selections:Seq[FundEngagementReportTypeSelectionData]):Option[(String,String,String)] = {
    val result = getFirstMappingInConfirmationReport(reportID,filterFunc,selections)
    if(result.isEmpty){
      None
    }else{
      var firstKey:Option[String] = None
      var JandP = ("","")
      for(k <- result.keys){
        if(firstKey.isEmpty){
          firstKey = Some(k)
          JandP = getFunc(result.get(k).get)
        }
      }
      Some(JandP._1,JandP._2,firstKey.get)
    }
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
    workCashInfos.foreach(workCashInfo => {
      if(columns.isEmpty){
        columns = getWorkCashColumnsFromReport(workCashInfo.reportId,workCashInfo.filterFunc,workCashInfo.getFunc,selections)
      }
    })


    columns match {
      case Some(value) => value
      case None => (missingStr(glAccountName),missingStr(glAccountName),EMPTY_STR)
    }
  }

  def guessingWorkInvJandP(workInvRow:Map[String, String],selections:Seq[FundEngagementReportTypeSelectionData]) = {

    val workInvInfos = generateWorkInvInfos(workInvRow)


    /**
     * Lack of item17
     */
    var columns:Option[(String,String,String)] =  None

    val custodian = workInvRow.get(WorkInvColumns.CUSTODIAN).getOrElse("")
    workInvInfos.foreach(workInvInfo => {
      if(columns.isEmpty){
        columns = getWorkInvColumnsFromReport(workInvInfo.reportId,workInvInfo.filterFunc,workInvInfo.getFunc,selections)
      }
    })


    columns match {
      case Some(value) => value
      case None => (missingStr(custodian),missingStr(custodian),EMPTY_STR)
    }
  }

  def generateWorkCashInfos(workCashRow:Map[String, String]) = {
    val egaQuantity = workCashRow.get(WorkCashColumns.ACCOUNT_BALANCE_IN_SOURCE_CURRENCY).getOrElse("")
    val currency = workCashRow.getOrElse(WorkCashColumns.BANK_ACCOUNT_CURRENCY, "")

    val item235Id = "3003"
    val item235Func = (ob:JSONObject)=>{
      ob.getString("Currency").trim == currency.trim && convertStringToNumbersAndEqual(ob.getString("Balance"),egaQuantity)
    }
    val item235GetFunc = (ob:JSONObject)=>{
      (ob.getString("Account No.") , ob.getString("Balance"))
    }

    /**
     * issues for 3001: content not right.
     */
    val collateralPositionByAgreementId = "3001"
    val collateralPositionByAgreementFunc = (ob:JSONObject)=>{
      ob.getString("Instrument Ccy").trim == currency.trim && convertStringToNumbersAndEqual(ob.getString("Notional"),egaQuantity)
    }
    val collateralPositionByAgreementGetFunc = (ob:JSONObject)=>{
      (NA_STR  , ob.getString("Notional"))
    }

    val ibanId = "3009"
    val ibanFunc = (ob:JSONObject)=>{
      ob.getString("ccy").trim == currency.trim && convertStringToNumbersAndEqual(ob.getString("balance"),egaQuantity)
    }
    val ibanGetFunc = (ob:JSONObject)=>{
      (ob.getString("iban") , ob.getString("balance"))
    }

    /**
     * Lack of item17
     */

    val workCashInfos = Seq(
      WorkCashInfo(item235Id,item235Func,item235GetFunc),
      //WorkCashInfo(collateralPositionByAgreementId,collateralPositionByAgreementFunc,collateralPositionByAgreementGetFunc),
      WorkCashInfo(ibanId,ibanFunc,ibanGetFunc)
    )
    workCashInfos
  }

  def generateWorkInvInfos(workInvRow:Map[String, String]) = {
    val description = workInvRow.get(WorkInvColumns.DESCRIPTION).getOrElse("")
    val currency = workInvRow.get(WorkInvColumns.ORI_CCY_RATE).getOrElse("")
    val fairValue = workInvRow.get(WorkInvColumns.FAIR_VALUE_PER_POSITION_REPORT).getOrElse("")
    val quantity = workInvRow.get(WorkInvColumns.QUANTITY).getOrElse("")

    /**
     * Issues for 3000: What is Quantity SD? and items are not qualified.
     */
    val creditDefaultSwapId = "3000"
    val creditDefaultSwapFilterFunc = (ob:JSONObject)=>{
      ob.getString("Product Description").trim == description.trim && convertStringToNumbersAndEqual(ob.getString("Currency"),currency) && convertStringToNumbersAndEqual(ob.getString("NOTIONAL"),fairValue)
    }
    val creditDefaultSwapGetFunc = (ob:JSONObject)=>{
      (NA_STR , ob.getString("NOTIONAL"))
    }

    /**
     * Issue for 3002: desciption should be ~=.
     */
    val otcExposureId = "3002"
    val otcExposureFilterFunc = (ob:JSONObject)=>{
      (ob.getString("Source Reference").trim == description.trim || ob.getString("Underlying Name").trim == description.trim) && ob.getString("Agreement Ccy").trim == currency.trim && convertStringToNumbersAndEqual(ob.getString("Exposure (Agree Ccy)"),fairValue)
    }
    val otcExposureGetFunc = (ob:JSONObject)=>{
      (ob.getString("Exposure (Agree Ccy)") , NA_STR)
    }


    /**
     * Issue for 3004: No 3004 example.
     */
    val item4Id = "3004"
    val item4FilterFunc = (ob:JSONObject)=>{
      ob.getString("facility").trim == description.trim && ob.getString("CCY") == currency && convertStringToNumbersAndEqual(ob.getString("Amount"),quantity)
    }
    val item4GetFunc = (ob:JSONObject)=>{
      (NA_STR, NA_STR)
    }

    /**
     * 3005 seems good.
     */
    val moneyMarketId = "3005"
    val moneyMarketFilterFunc = (ob:JSONObject)=>{
      ob.getString("CCY").trim == currency.trim && convertStringToNumbersAndEqual(ob.getString("Amount"),quantity)
    }
    val moneyMarketGetFunc = (ob:JSONObject)=>{
      (ob.getString("Amount") , ob.getString("Amount"))
    }

    /**
     * Issues for 3006: Seems content is not consistent with doc.
     */
    val foreignExchangeId = "3006"
    val foreignExchangeFilterFunc = (ob:JSONObject)=>{
      ob.getString("CCY").trim == currency.trim && convertStringToNumbersAndEqual(ob.getString("Amount"),quantity)
    }
    val foreignExchangeGetFunc = (ob:JSONObject)=>{
      (NA_STR, ob.getString("Amount"))
    }

    /**
     * Issues for 3007: TODO.
     */
    val item17Id = "3007"
    val item17FilterFunc = (ob:JSONObject)=>{
      ob.getString("CCY").trim == currency.trim && ob.getString("Amount") == quantity
    }
    val item17GetFunc = (ob:JSONObject)=>{
      (NA_STR, ob.getString("Amount"))
    }

    val workInvInfos = Seq(
      WorkInvInfo(creditDefaultSwapId,creditDefaultSwapFilterFunc,creditDefaultSwapGetFunc),
      WorkInvInfo(otcExposureId,otcExposureFilterFunc,otcExposureGetFunc) ,
      WorkInvInfo(item4Id,item4FilterFunc,item4GetFunc),
      WorkInvInfo(foreignExchangeId,foreignExchangeFilterFunc,foreignExchangeGetFunc),
      WorkInvInfo(moneyMarketId,moneyMarketFilterFunc,moneyMarketGetFunc),
      WorkInvInfo(item17Id,item17FilterFunc,item17GetFunc),
    )
    workInvInfos
  }

  case class WorkInvInfo(reportId:String, filterFunc:(JSONObject)=>Boolean, getFunc:(JSONObject)=>(String,String))
  case class WorkCashInfo(reportId:String, filterFunc:(JSONObject)=>Boolean, getFunc:(JSONObject)=>(String,String))
}