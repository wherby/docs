import java.io.File
import scala.concurrent.Await

object BasicProcessFile {

  def getStorageBase(singleEGAStorage: GeneralSingleEGAStorage)(
    selection: Seq[FundEngagementReportTypeSelectionData],
    sheetTypesReportTypesMapRead: SheetTypesReportTypesMapRead
  ) = {
    GeneralSingleEGAHelper.rowToStorage(selection, singleEGAStorage, sheetTypesReportTypesMapRead)
    singleEGAStorage
  }

  def extractFileAndStoreDataInDB(
                                   content: String,
                                   fundEngagement: FundEngagementData,
                                   fund: Fund,
                                   targetFilePath: String,
                                   userEmail: String,
                                   methodName: String,
                                   multifondsUploadHelper: MultifondsUploadHelper,
                                   doraServer: DoraServer,
                                   fundEngagementReportTypeSelectionRead: FundEngagementReportTypeSelectionRead,
                                   fundEngagementReportTypeSelectionWrite: FundEngagementReportTypeSelectionWrite,
                                   sheetTypesReportTypesMapRead: SheetTypesReportTypesMapRead,
                                   disabledAfterYEReports: Map[String, Boolean],
                                   someExtractor: Map[Seq[String], (AbstractProcessor, ExtractorConfig)]
                                 ): Result = {
    //filter the extractorMap with report header
    //var someExtractor = morganExtractorMap

    var warningMsg: Seq[Result] = Seq()
    var exceptions: Seq[Result] = Seq()
    var noWarning: Seq[Result]  = Seq()
    var allEmpty                = true
    someExtractor.map((kv) => {
      val periodStart = fund.auditPeriodBegin
      val periodEnd   = fund.auditPeriodEnd
      val paras       = List(targetFilePath, kv._2._2, fund.name, periodStart, periodEnd)
      val result = doraServer.runProcess(
        paras,
        kv._2._1.getClass.getCanonicalName.stripSuffix("$"),
        methodName
      )
      val newStorage = Await.result(result, ConstVar.ProcessJobWaitTime seconds)
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
                100 seconds
              )
              if (newStorageResult.result.asInstanceOf[GeneralSingleEGAStorage].errorMsg.hasError) {
                return BadRequest(
                  newStorageResult.result.asInstanceOf[GeneralSingleEGAStorage].errorMsg.Msg
                )
              }
              val res = Ok(
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
          encapErrorResponse(
            UnprocessableEntity,
            e.errorCode
          )
        }
        case e: Exception => {
          if (e.getMessage.indexOf("no newline character detected") < 0) {
            encapErrorResponse(
              UnprocessableEntity,
              ExtractionErrorCode.UNRECOGNISED_ERROR
            )
          }
        }
      }
    })

    if (exceptions.length > 0) {
      exceptions(0)
    } else if (warningMsg.length > 0) {
      warningMsg(0)
    } else if (allEmpty == true) {
      encapErrorResponse(
        UnprocessableEntity,
        ExtractionErrorCode.UNRECOGNISED_ERROR
      )
    } else {
      noWarning(0)
    }
  }

  def processFiles(someExtractor: Map[Seq[String], (AbstractProcessor, ExtractorConfig)])(
    request: Request[MultipartFormData[Files.TemporaryFile]],
    fundEngagement: FundEngagementData,
    fund: Fund,
    multifondsUploadHelper: MultifondsUploadHelper,
    doraServer: DoraServer,
    fundEngagementReportTypeSelectionRead: FundEngagementReportTypeSelectionRead,
    fundEngagementReportTypeSelectionWrite: FundEngagementReportTypeSelectionWrite,
    sheetTypesReportTypesMapRead: SheetTypesReportTypesMapRead,
    disabledAfterYEReports: Map[String, Boolean]
  ): Result = {
    request.body
      .file("file")
      .map(file => {
        val userEmail  = request.session.get("email").getOrElse("")
        val sourceFile = file.ref.toFile
        val filenames  = file.filename.split('.')
        if (filenames.length <= 1) {
          encapErrorResponse(
            UnprocessableEntity,
            ExtractionErrorCode.UNSUPPORTED_REPORT_TYPE_FILE_EXTENSION
          )
        } else {
          val targetFile = new File(s"${sourceFile.getPath}.${filenames.last}")
          sourceFile.renameTo(targetFile)
          var methodName = ""

          if (filenames.last == "xlsx" || filenames.last == "xlsm" || filenames.last == "xls") {
            methodName = "readFileAndExtractToObjectForPeriod"
          } else if (filenames.last == "pdf") {
            methodName = "readPDFFileAndExtractToObject"
          } else {
            methodName = "readCSVFileAndExtractToObject"
          }

          extractFileAndStoreDataInDB(
            file.filename,
            fundEngagement,
            fund,
            targetFile.getPath,
            userEmail,
            methodName,
            multifondsUploadHelper,
            doraServer,
            fundEngagementReportTypeSelectionRead,
            fundEngagementReportTypeSelectionWrite,
            sheetTypesReportTypesMapRead,
            disabledAfterYEReports,
            someExtractor
          )
        }
      })
      .getOrElse(
        encapErrorResponse(
          UnprocessableEntity,
          ExtractionErrorCode.UNRECOGNISED_ERROR
        )
      )
  }
}