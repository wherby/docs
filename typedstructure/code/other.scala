def exportHSBCConfirmationToDatabase(user: String, fileName: String, storage: HSBCConfirmationSingleEGAStorage, fundEngagementId: String, userEmail: String,
                                     fundEngagementReportTypeSelectionRead: FundEngagementReportTypeSelectionRead,
                                     fundEngagementReportTypeSelectionWrite: FundEngagementReportTypeSelectionWrite,
                                     egaRead: EgaRead,
                                     egaWrite: EgaWrite
                                    ) = {
  var selection: Seq[FundEngagementReportTypeSelectionData] = Await.result(fundEngagementReportTypeSelectionRead.lookupByFundEngagementId(fundEngagementId), 10 seconds)
  val selectionDataMap: Map[String, Seq[scala.collection.Map[String, String]]] = Map(
    "3000" -> storage.creditDefaultSwapData,
    "3001" -> storage.collateralPositionData,
    "3002" -> storage.OTCExposureTradesData,
    "3003" -> storage.itemTwoThreeFiveData,
    "3004" -> storage.itemFourData,
    "3005" -> storage.moneyMarketDealData,
    "3006" -> storage.foreignExchangeDealData,
    "3007" -> storage.itemSeventeenData,
    "3008" -> storage.statementDate,
    "3009" -> storage.ibanData
  )

  selectionDataMap.filter(p => {
    Integer.parseInt(p._1) != 3008
  }).map(selection => {
    var extraction: Seq[scala.collection.Map[String, String]] = selection._2
    var cashNum = 0
    var invNum = 0

    if (extraction.length > 0) {
      //get workCash or workInv
      selection._1 match {
        case "3000" => {
          var num = extraction.length
          invNum = invNum + num
        }
        case "3002" => {
          var num = extraction.length
          invNum = invNum + num
        }
        case "3004" => {
          var num = extraction.length
          invNum = invNum + num
        }
        case "3005" => {
          var num = extraction.length
          invNum = invNum + num
        }
        case "3006" => {
          var num = extraction.length
          invNum = invNum + num
        }
        case _ => {
          var num = extraction.length
          cashNum = cashNum + num
        }
      }
    }

    //save or update the etc column which stores the workcash, workinv, filename and answers
    var etcFuture = egaRead.lookupByFundEngagementId(fundEngagementId).map(maybeEga => {
      maybeEga match {
        case Some(value) => {
          value.etc.getOrElse("")
        }
        case None => {
          ""
        }
      }
    })
    var etcString = Await.result(etcFuture, 10 seconds)
    var etcNewArray = new JSONArray()

    if (etcString.length > 0 && etcString != "[]") {
      var etcArray = new JSONArray(etcString)
      var found = 0
      for (num <- 0 to etcArray.length() - 1) {
        var obj = etcArray.getJSONObject(num)
        var name = obj.get("fileName")
        if (name == fileName) {
          found = 1
          var etcNewObj = new JSONObject()
          if (obj.toString.contains("answers")) {
            etcNewObj.put("answers", obj.get("answers"))
          } else {
            etcNewObj.put("answers", "")
          }
          etcNewObj.put("fileName", obj.get("fileName"))
          if (obj.toString.contains("workCash")) {
            var workCashNum = Integer.parseInt(obj.get("workCash").toString)
            var workInvNum = Integer.parseInt(obj.get("workInv").toString)
            cashNum = cashNum + workCashNum
            invNum = invNum + workInvNum
          }
          etcNewObj.put("workCash", cashNum)
          etcNewObj.put("workInv", invNum)
          etcNewArray.put(etcNewObj)
        } else {
          etcNewArray.put(obj)
        }
      }
      if (found == 0) {
        var etcNewObj = new JSONObject()
        etcNewObj.put("workCash", cashNum)
        etcNewObj.put("workInv", invNum)
        etcNewObj.put("fileName", fileName)
        etcNewObj.put("answers", "")
        etcNewArray.put(etcNewObj)
      }
    } else {
      var etcNewObj = new JSONObject()
      etcNewObj.put("workCash", cashNum)
      etcNewObj.put("workInv", invNum)
      etcNewObj.put("fileName", fileName)
      etcNewObj.put("answers", "")
      etcNewArray.put(etcNewObj)
    }

    var writeEtcFuture = egaRead.lookupByFundEngagementId(fundEngagementId).flatMap {
      egaOption =>
        egaOption match {
          case Some(egaData) => {
            var newData = egaData

            newData = egaData.copy(etc = Some(etcNewArray.toString()), modifydatetime = Some(new Timestamp(DateTime.now.getMillis)))
            egaWrite.update(newData)
          }
          case _ => {
            var egaIdOpt = Some(randomUUID().toString)
            var newData = EgaData(egaIdOpt.get, fundEngagementId, GenerateStatus.not_generated.toString, None, null, Some(userEmail), Some(new Timestamp(DateTime.now.getMillis)))
            newData = newData.copy(etc = Some(etcNewArray.toString()))
            egaWrite.create(newData)
          }
        }
    }
    Await.result(writeEtcFuture, 10 seconds)
  })

  //update newUploadFileContent
  val newRows: Seq[Option[FundEngagementReportTypeSelectionData]] = selection.filter(p => {
    Integer.parseInt(p.sheettypesReporttypesMapId) >= 3000
  }).map(row => {

    var previousContent: String = row.extractionFileContent.getOrElse("")
    var previousUploadContent = row.uploadFileContent.getOrElse("{}")
    var extracted: Seq[scala.collection.Map[String, String]] = selectionDataMap.get(row.sheettypesReporttypesMapId).getOrElse(Seq())
    var extractedString = ""

    var newArray = new JSONArray()
    var extractedObject = new JSONObject()

    var array = new JSONArray()
    if (extracted.length > 0) {
      val newUploadFileContent = if (row.sheettypesReporttypesMapId == "3003") {
        var previousAccountNumberFileNameMapping = Json.parse(previousUploadContent).as[Map[String, String]]
        val currentAccountNumberFileNameMapping = if (extracted.length > 0) {
          for (num <- 0 to extracted.length - 1) {
            previousAccountNumberFileNameMapping = previousAccountNumberFileNameMapping + (extracted(num).apply("Account No.").trim -> fileName.trim)
          }
          previousAccountNumberFileNameMapping
        } else {
          previousAccountNumberFileNameMapping
        }
        Some(Json.stringify(Json.toJson(currentAccountNumberFileNameMapping)))
      } else {
        row.uploadFileContent
      }

      extracted.map(v => {
        for (key <- v.keys) {
          var obj = new JSONObject()
          obj.put(key, v.get(key).getOrElse(""))
          array.put(obj)
        }
      })
      var date = new java.util.Date();
      var dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);
      extractedObject.put(fileName + "|" + user + "|" + dateFormat.format(date), Json.toJson(extracted).toString())
      if (previousContent.length > 0 && previousContent != "[]") {
        var previousArray = new JSONArray(previousContent)
        previousArray.put(extractedObject)
        extractedString = previousArray.toString
      } else {
        newArray.put(extractedObject)
        extractedString = newArray.toString()
      }
      val newRow = row.copy(uploadFileContent = newUploadFileContent, uploadFileStatus = UploadFileStatus.success.toString, extractionFileContent = Some(extractedString), extractionStatus = ExtractStatus.success.toString, modifyby = Some(userEmail), modifydatetime = Some(new Timestamp(System.currentTimeMillis())))
      Some(newRow)
    } else {
      None
    }
  })

  fundEngagementReportTypeSelectionWrite.updateSelectionBatch(newRows.filter(row => !row.isEmpty).map(row => row.get))

}