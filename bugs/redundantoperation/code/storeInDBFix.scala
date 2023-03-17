def exportExcelToDatabase(
                           storage: GeneralSingleEGAStorage,
                           fundEngagementId: String,
                           userEmail: String,
                           fundEngagementReportTypeSelectionRead: FundEngagementReportTypeSelectionRead,
                           fundEngagementReportTypeSelectionWrite: FundEngagementReportTypeSelectionWrite,
                           sheetTypesReportTypesMapRead: SheetTypesReportTypesMapRead
                         ) = {
  if(!storage.isStorageNotEmpty()){
    Future(Unit)
    //Do nothing if no value updated.
  }else{
    val selections: Seq[FundEngagementReportTypeSelectionData] = Await.result(
      fundEngagementReportTypeSelectionRead.lookupByFundEngagementId(fundEngagementId),
      10 seconds
    )
    val selectionDataMap: Map[String, String] = selections.map { selection =>
      val sheetTypeId = Await
        .result(
          sheetTypesReportTypesMapRead.lookupByID(selection.sheettypesReporttypesMapId),
          10 seconds
        )
        .map(_.sheetTypeId)
        .getOrElse("-1")
      val value = sheetTypeId match {
        case "1" => Json.toJson(storage.trialBalanceReportTBData).toString()
        case "2" => Json.toJson(storage.detailedGeneralLedgerReportGLData).toString()
        case "3" => Json.toJson(storage.detailedGeneralLedgerReportGLAfterYEData).toString()
        case "4" => Json.toJson(storage.positionAppraisalReportPositionData).toString()
        case "5" => Json.toJson(storage.cashAppraisalReportCashData).toString()
        case "6" => Json.toJson(storage.purchaseAndSaleTransactionReportPSRData).toString()
        case "7" => Json.toJson(storage.purchaseAndSaleTransactionReportPSRAfterYEData).toString()
        case "8" => Json.toJson(storage.dividendsIncomeExpenseReportDividendData).toString()
        case "9" => Json.toJson(storage.dividendsIncomeExpenseReportDividendAfterYEData).toString()
        case "10" =>
          Json.toJson(storage.realisedGainLossReportRGLData).toString()
        case "11" => Json.toJson(storage.realisedGainLossReportRGLAfterYEData).toString()
        case _    => ""
      }
      selection.sheettypesReporttypesMapId -> value
    }.toMap
    val addtionMap = storage.additionMap.toSeq.map { kv =>
      (kv._1 -> Json.toJson(kv._2).toString())
    }
    val mergedSelectionMap = selectionDataMap ++ addtionMap
    val newRows: Seq[Option[FundEngagementReportTypeSelectionData]] = selections.map(row => {
      val extractedString: String =
        mergedSelectionMap.get(row.sheettypesReporttypesMapId).getOrElse("")
      if (extractedString.length > 0 && extractedString != "[]") {
        val newRow = row.copy(
          uploadFileContent = None,
          uploadFileStatus = UploadFileStatus.success.toString,
          extractionFileContent = Some(extractedString),
          extractionStatus = ExtractStatus.success.toString,
          modifyby = Some(userEmail),
          modifydatetime = Some(new Timestamp(System.currentTimeMillis()))
        )
        Some(newRow)
      } else {
        None
      }
    })
    Await.result(
      fundEngagementReportTypeSelectionWrite.updateSelectionBatch(
        newRows.filter(row => !row.isEmpty).map(row => row.get)
      ),
      10 seconds
    )
    fundEngagementReportTypeSelectionWrite.writeReadyTime(fundEngagementId)
  }

}