//write BNY Confirmation file results to DB
def exportBNYConfirmationToDatabase(user: String, fileName: String, storage: BNYConfirmationSingleEGAStorage, fundEngagementId: String, userEmail: String,
                                    fundEngagementReportTypeSelectionRead: FundEngagementReportTypeSelectionRead,
                                    fundEngagementReportTypeSelectionWrite: FundEngagementReportTypeSelectionWrite,
                                    egaRead: EgaRead,
                                    egaWrite: EgaWrite
                                   ) = {
  var selection: Seq[FundEngagementReportTypeSelectionData] = Await.result(fundEngagementReportTypeSelectionRead.lookupByFundEngagementId(fundEngagementId), 10 seconds)
  val selectionDataMap: Map[String, Seq[ConfirmationFile]] = Map(
    "7001" -> storage.consolodatedCashData,
    "7002" -> storage.assetHoldData
  )

  selectionDataMap.map(selection => {
    var extraction: Seq[ConfirmationFile] = selection._2
    if(extraction.length > 0) {
      var dateFromFile = extraction(0).date
      var records = extraction(0).records
      var cashNum = 0
      var invNum = 0

      records.map(rec => {
        if(rec.balance.getOrElse("") != "") {
          cashNum = cashNum + 1
          invNum = invNum + 1
        }
      })

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
    }
  })

  //update newUploadFileContent
  val newRows: Seq[Option[FundEngagementReportTypeSelectionData]] = selection.filter(p => {
    Integer.parseInt(p.sheettypesReporttypesMapId) >= 7000
  }).map(row => {

    var previousContent: String = row.extractionFileContent.getOrElse("")
    var previousUploadContent = row.uploadFileContent.getOrElse("{}")
    var extracted: Seq[ConfirmationFile] = selectionDataMap.get(row.sheettypesReporttypesMapId).getOrElse(Seq())
    if(extracted.length > 0) {
      var extractedString = ""

      var extractedObject = new JSONObject()
      var newObject = new JSONObject()

      var array = new JSONArray()
      if (extracted.length > 0) {
        var records = extracted(0).records
        var dateFromFile = extracted(0).date.getOrElse("").trim

        var previousAccountNumberFileNameMapping = Json.parse(previousUploadContent).as[Map[String, String]]
        val currentAccountNumberFileNameMapping = if (records.length > 0) {
          for (num <- 0 to records.length - 1) {
            previousAccountNumberFileNameMapping = previousAccountNumberFileNameMapping + ( records(num).accountNumber.toString -> fileName.trim)
          }
          previousAccountNumberFileNameMapping

        } else {
          previousAccountNumberFileNameMapping
        }
        val newUploadFileContent = Some(Json.stringify(Json.toJson(currentAccountNumberFileNameMapping)))

        records.map(v => {
          var obj = new JSONObject()
          obj.put("accountNumber", v.accountNumber.getOrElse(""))
          obj.put("balance", v.balance.getOrElse(""))
          obj.put("currency", v.currency.getOrElse(""))
          obj.put("description", v.descriptionOrISIN.getOrElse(""))
          obj.put("etc", v.etc.getOrElse(""))
          obj.put("quantity", v.quantity.getOrElse(""))
          array.put(obj)
        })

        extractedObject.put(fileName + "|" + user + "|" + dateFromFile, array.toString())
        var newKey = fileName + "|" + user + "|" + dateFromFile
        var found = false

        if (previousContent.length > 0 && previousContent != "[]") {
          var preObj = new JSONObject(previousContent)
          var keys = preObj.keys()
          while(keys.hasNext()) {
            var key = keys.next().toString
            var value = preObj.get(key)
            if(key == newKey) {
              newObject.put(newKey, array.toString())
              found = true
            } else {
              newObject.put(key, value.toString)
            }
          }
          if(!found) {
            newObject.put(newKey, array.toString)
          }
          extractedString = newObject.toString
        } else {
          extractedString = extractedObject.toString()
        }
        val newRow = row.copy(uploadFileContent = newUploadFileContent, uploadFileStatus = UploadFileStatus.success.toString, extractionFileContent = Some(extractedString), extractionStatus = ExtractStatus.success.toString, modifyby = Some(userEmail), modifydatetime = Some(new Timestamp(System.currentTimeMillis())))
        Some(newRow)
      } else {
        None
      }
    } else {
      None
    }
  })
  fundEngagementReportTypeSelectionWrite.updateSelectionBatch(newRows.filter(row => !row.isEmpty).map(row => row.get))
}