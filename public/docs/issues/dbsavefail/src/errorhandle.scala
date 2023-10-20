      try {
        //check whether the result has exception
        multifondsUploadHelper.checkJobResultException(newStorage)
        if (newStorage.result.isInstanceOf[ProcessResult]) {
          val newStorageResult = newStorage.result.asInstanceOf[ProcessResult]
          if (newStorageResult.jobStatus.toString != "Failed") {
            if (newStorageResult.result.isInstanceOf[GeneralSingleEGAStorage]) {
              if (
                newStorageResult.result.asInstanceOf[GeneralSingleEGAStorage].isStorageNotEmpty()
              ) {
                allEmpty = false
              }
              filterAfterYEDataSH(
                newStorageResult.result.asInstanceOf[GeneralSingleEGAStorage],
                disabledAfterYEReports
              )
              //store the extracted data in database
              Await.result(
                GeneralSingleEGAHelper.exportExcelToDatabase(
                  newStorageResult.result.asInstanceOf[GeneralSingleEGAStorage],
                  fundEngagement.id,
                  userEmail,
                  fundEngagementReportTypeSelectionRead,
                  fundEngagementReportTypeSelectionWrite,
                  sheetTypesReportTypesMapRead
                ),
                ConstVar.ProcessJobWaitTimeDuration
              )
              if (newStorageResult.result.asInstanceOf[GeneralSingleEGAStorage].errorMsg.hasError) {
                return AppResult.badRequest(
                  newStorageResult.result.asInstanceOf[GeneralSingleEGAStorage].errorMsg.Msg
                )
              }
              val res = AppResult.okResult(
                "Extract Success" + "," + newStorageResult.result
                  .asInstanceOf[GeneralSingleEGAStorage]
                  .warningMsg
                  .Msg + "," + fund.name + "," + periodStart + "," + periodEnd
              )
              if (
                newStorageResult.result
                  .asInstanceOf[GeneralSingleEGAStorage]
                  .warningMsg
                  .Msg
                  .length > 0
              ) {
                val warningMsgs =
                  newStorageResult.result.asInstanceOf[GeneralSingleEGAStorage].warningMsg
                warningMsg = ModelChangeHelper
                  .handleOldWarningMsgToWarningObject(warningMsgs, warningMsg, fund)
              } else {
                noWarning = noWarning :+ res
              }
            } else {
              exceptions :+= encapErrorResponse(
                UnprocessableEntity,
                ExtractionErrorCode.UNSUPPORTED_REPORT_TYPE_FILE_EXTENSION
              )

            }
          }
        } else {
          exceptions :+= encapErrorResponse(
            UnprocessableEntity,
            ExtractionErrorCode.UNRECOGNISED_ERROR
          )
        }
      } catch {
//        case e:ExtractionWarning=>{
//          Ok( Json.obj("errorCodes" -> Seq(e.errorCode).distinct))
//        }
        case e: ExtractionError => {
          exceptions :+= encapErrorResponse(
            UnprocessableEntity,
            e.errorCode
          )
        }
        case e: Exception => {
          if (
            (e.getMessage.indexOf("no newline character detected") >= 0 && targetFilePath
              .toLowerCase()
              .indexOf("pdf") < 0) || (e.getMessage.indexOf("head of empty list") >= 0)
          ) {} else {
            exceptions :+= encapErrorResponse(
              UnprocessableEntity,
              ExtractionErrorCode.UNRECOGNISED_ERROR
            )
          }
        }
      }
    })