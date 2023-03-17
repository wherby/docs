import java.io.ByteArrayOutputStream
import java.util.UUID.randomUUID
import javax.sql.rowset.serial.SerialBlob
import scala.concurrent.Await

def generateSingleEGA(fundEngagementId: String, templateId: String) =
  deadbolt.SubjectPresent()(parse.anyContent) { implicit request =>
  {

    val userEmail = request.session.get("email").getOrElse("")
    SafeLogger.logString(
      Logger,
      "EXECUTING GENERATE SINGLE EGA FOR " + fundEngagementId + " AT " + TimeService.currentTime.toString
    )
    var selection: Seq[FundEngagementReportTypeSelectionData] = Await.result(
      fundEngagementReportTypeSelectionRead.lookupByFundEngagementId(fundEngagementId),
      10 seconds
    )
    var ega = Await.result(egaRead.lookupByFundEngagementId(fundEngagementId), 10 seconds)
    val (securityItems, questionaireStr) = ega match {
      case Some(value) => (value.securityItems, value.etc)
      case None        => (None, None)
    }

    val currentFundEngagement =
      Await.result(fundEngagementRead.lookup(fundEngagementId), 10 seconds)
    val defaultDate = new java.sql.Date(new java.util.Date().getTime)
    //      val (currentFundName, currentFundAdmin, currentFundAuditPeriodEnd, currentBaseCurrency) = currentFundEngagement match {
    //        case Some(value) => {
    //          val currentFund = Await.result(fundsRead.lookup(value.fundid), 10 seconds)
    //          val (fundName, fundAdmin, fundAuditPeriodEnd, baseCurrency) = currentFund match {
    //            case Some(value) => (value.name, value.fundAdmin, value.auditPeriodEnd, value.baseCurrency)
    //            case None => ("", "", defaultDate, "")
    //          }
    //          (fundName, fundAdmin, fundAuditPeriodEnd, baseCurrency)
    //        }
    //        case None => ("", "", defaultDate, "")
    //      }
    val fundRecordOpt = currentFundEngagement.flatMap { fEngagement =>
      fEngagement.fundRecord.flatMap { record =>
        Json.parse(record).asOpt[Fund]
      }
    }
    val (currentFundName, currentFundAdmin, currentFundAuditPeriodEnd, currentBaseCurrency) =
      fundRecordOpt match {
        case Some(value) =>
          (value.name, value.fundAdmin, value.auditPeriodEnd, value.baseCurrency)
        case None => ("", "", defaultDate, "")
      }
    val accountRecordWithRef = ega
      .flatMap(_.accounts.map(accountStr => {
        Json.parse(accountStr).as[Seq[Map[String, String]]]
      }))
      .getOrElse(Seq())
    val accountMapping = Await.result(fundAccountRead.lookupByFund(currentFundName), 10 seconds)
    val securityItemsAndPrices =
      Json.parse(securityItems.getOrElse("{}")).as[SecurityItemsAndPricesTemp]
    val questionare = Json.parse(questionaireStr.getOrElse("[]")).as[JsArray]
    val accountNumberFileMapping = selection
      .filter(select =>
        select.uploadFileContent.isDefined && (select.sheettypesReporttypesMapId == "42" || select.sheettypesReporttypesMapId == "5001")
      )
      .map(row => Json.parse(row.uploadFileContent.get).as[Map[String, String]])
      .foldLeft(Map[String, String]())((a, b) => a ++ b)
    val workInvDataGUESSINGSOURCE = selection
      .filter(select =>
        select.uploadFileContent.isDefined && (select.sheettypesReporttypesMapId == "43" || select.sheettypesReporttypesMapId == "44" || select.sheettypesReporttypesMapId == "45" || select.sheettypesReporttypesMapId == "46" || select.sheettypesReporttypesMapId == "47" || select.sheettypesReporttypesMapId == "48")
      )
      .map(row =>
        row.sheettypesReporttypesMapId -> Json
          .parse(row.uploadFileContent.get)
          .as[Map[String, Seq[Map[String, String]]]]
      )
      .toMap

    singleEGATemplateRead
      .lookup(templateId)
      .map(maybeTemp => {
        maybeTemp match {
          case Some(value) => {
            val templateContent = value.templateContent
            templateContent match {
              case Some(content) => {
                (Some(convertBlobToWorkbook(content)), value.templateComment)
              }
              case None => (None, value.templateComment)
            }
          }
          case None => (None, Some(genericTemplateComment))
        }
      })
      .flatMap(res => {
        val template               = res._1
        val comment                = res._2
        val bstream                = new ByteArrayOutputStream()
        var workbook: XSSFWorkbook = null
        try {
          val templateWorkbook = template match {
            case Some(tempContent) => tempContent
            case None => {
              getWorkbookFromDefaultSingleEGATemplate()
            }
          }
          SafeLogger.logString(Logger, "BEGIN EXPORT: " + TimeService.currentTime.toString)
          var outputParams: Seq[OutPutParam] = Seq()
          currentFundAdmin match {
            case HSBC_IMS => {
              val singleEGAStorage = this.getIMSStorage(selection)
              workbook = singleEGAStorage.exportToExcelWorkbook(
                currentFundAuditPeriodEnd,
                currentFundName,
                currentBaseCurrency,
                templateWorkbook
              )
              val cashParam     = singleEGAStorage.cashParam.sheetContent()
              val positionParam = singleEGAStorage.positionParam.sheetContent()
              workbook = Confirmation.generateConfirmationAndValuationToWorkbook(
                selection,
                cashParam,
                positionParam,
                accountMapping,
                accountRecordWithRef,
                questionare,
                accountNumberFileMapping,
                securityItemsAndPrices,
                currentFundAuditPeriodEnd,
                (cNames: Seq[String], periodEndDate: String) => {
                  securityPriceRead
                    .lookupSecurityItemAndPriceByIsinAndPeriodEndDate(cNames, periodEndDate)
                },
                workInvDataGUESSINGSOURCE,
                workbook,
                securityItemRead
              )
              outputParams = singleEGAStorage.outputParams
            }
            case HSBC_Multifonds => {
              val singleEGAStorage = this.getMultifundsStorage(selection)
              workbook = singleEGAStorage.exportToExcelWorkbook(
                currentFundAuditPeriodEnd,
                currentFundName,
                currentBaseCurrency,
                templateWorkbook
              )
              val cashParam     = singleEGAStorage.cashParam.sheetContent()
              val positionParam = singleEGAStorage.positionParam.sheetContent()
              workbook = Confirmation.generateConfirmationAndValuationToWorkbook(
                selection,
                cashParam,
                positionParam,
                accountMapping,
                accountRecordWithRef,
                questionare,
                accountNumberFileMapping,
                securityItemsAndPrices,
                currentFundAuditPeriodEnd,
                (cNames: Seq[String], periodEndDate: String) => {
                  securityPriceRead
                    .lookupSecurityItemAndPriceByIsinAndPeriodEndDate(cNames, periodEndDate)
                },
                workInvDataGUESSINGSOURCE,
                workbook,
                securityItemRead
              )
              outputParams = singleEGAStorage.outputParams
            }
            case HSBC_Geneva => {
              val singleEGAStorage = this.getGenevaStorage(selection)
              workbook = singleEGAStorage.exportToExcelWorkbook(
                currentFundAuditPeriodEnd,
                currentFundName,
                currentBaseCurrency,
                templateWorkbook
              )
              val cashParam     = singleEGAStorage.cashParam.sheetContent()
              val positionParam = singleEGAStorage.positionParam.sheetContent()
              workbook = Confirmation.generateConfirmationAndValuationToWorkbook(
                selection,
                cashParam,
                positionParam,
                accountMapping,
                accountRecordWithRef,
                questionare,
                accountNumberFileMapping,
                securityItemsAndPrices,
                currentFundAuditPeriodEnd,
                (cNames: Seq[String], periodEndDate: String) => {
                  securityPriceRead
                    .lookupSecurityItemAndPriceByIsinAndPeriodEndDate(cNames, periodEndDate)
                },
                workInvDataGUESSINGSOURCE,
                workbook,
                securityItemRead
              )
              outputParams = singleEGAStorage.outputParams
            }
            case SSC_Citi => {
              val singleEGAStorage = this.getSSCStorage(selection)
              workbook = SingleEGAExporterXLS.exportToExcelWorkbook(
                currentFundAuditPeriodEnd,
                currentFundName,
                currentBaseCurrency,
                templateWorkbook,
                singleEGAStorage.outputParams,
                singleEGAStorage.outputDateFormat
              )
              val cashParam     = singleEGAStorage.cashParam.sheetContent()
              val positionParam = singleEGAStorage.positionParam.sheetContent()
              workbook = Confirmation.generateConfirmationAndValuationToWorkbook(
                selection,
                cashParam,
                positionParam,
                accountMapping,
                accountRecordWithRef,
                questionare,
                accountNumberFileMapping,
                securityItemsAndPrices,
                currentFundAuditPeriodEnd,
                (cNames: Seq[String], periodEndDate: String) => {
                  securityPriceRead
                    .lookupSecurityItemAndPriceByIsinAndPeriodEndDate(cNames, periodEndDate)
                },
                workInvDataGUESSINGSOURCE,
                workbook,
                securityItemRead
              )
              /*val ca = new HSBCConfirmationValuation(selection, cashParam, positionParam, accountMapping, accountRecordWithRef,
            questionare, accountNumberFileMapping, securityItemsAndPrices, currentFundAuditPeriodEnd, (cNames: Seq[String], periodEndDate: String)
            => {
              securityPriceRead.lookupSecurityItemAndPriceByIsinAndPeriodEndDate(cNames, periodEndDate)
            }, workInvDataGUESSINGSOURCE)
          workbook = ca.exportConfirmation(workbook)*/
              outputParams = singleEGAStorage.outputParams
            }
            case SSC_WellsFargo => {
              val singleEGAStorage = WellsFargoProcessor.getFargoStorage(selection)
              workbook = SingleEGAExporterXLS.exportToExcelWorkbook(
                currentFundAuditPeriodEnd,
                currentFundName,
                currentBaseCurrency,
                templateWorkbook,
                singleEGAStorage.outputParams,
                singleEGAStorage.outputDateFormat
              )
              val cashParam     = singleEGAStorage.cashParam.sheetContent()
              val positionParam = singleEGAStorage.positionParam.sheetContent()
              workbook = Confirmation.generateConfirmationAndValuationToWorkbook(
                selection,
                cashParam,
                positionParam,
                accountMapping,
                accountRecordWithRef,
                questionare,
                accountNumberFileMapping,
                securityItemsAndPrices,
                currentFundAuditPeriodEnd,
                (cNames: Seq[String], periodEndDate: String) => {
                  securityPriceRead
                    .lookupSecurityItemAndPriceByIsinAndPeriodEndDate(cNames, periodEndDate)
                },
                workInvDataGUESSINGSOURCE,
                workbook,
                securityItemRead
              )
              outputParams = singleEGAStorage.outputParams
            }
            case Morgan_Stanley => {
              val singleEGAStorage = MorganProcessor.getStorage(selection)
              workbook = SingleEGAExporterXLS.exportToExcelWorkbook(
                currentFundAuditPeriodEnd,
                currentFundName,
                currentBaseCurrency,
                templateWorkbook,
                singleEGAStorage.outputParams,
                singleEGAStorage.outputDateFormat
              )
              val cashParam     = singleEGAStorage.cashParam.sheetContent()
              val positionParam = singleEGAStorage.positionParam.sheetContent()
              workbook = Confirmation.generateConfirmationAndValuationToWorkbook(
                selection,
                cashParam,
                positionParam,
                accountMapping,
                accountRecordWithRef,
                questionare,
                accountNumberFileMapping,
                securityItemsAndPrices,
                currentFundAuditPeriodEnd,
                (cNames: Seq[String], periodEndDate: String) => {
                  securityPriceRead
                    .lookupSecurityItemAndPriceByIsinAndPeriodEndDate(cNames, periodEndDate)
                },
                workInvDataGUESSINGSOURCE,
                workbook,
                securityItemRead
              )
              outputParams = singleEGAStorage.outputParams
            }
            case _ =>
              val fundStrategy: Option[FundReportStrategy] =
                fundReportStrategyFactory.getFundReportStrategy(currentFundAdmin)
              fundStrategy match {
                case Some(strategy) =>
                  workbook = strategy.generateSingleEGA(selection)(
                    currentFundAuditPeriodEnd,
                    currentFundName,
                    currentBaseCurrency,
                    templateWorkbook
                  )(
                    accountMapping,
                    accountRecordWithRef,
                    questionare,
                    accountNumberFileMapping,
                    securityItemsAndPrices,
                    (cNames: Seq[String], periodEndDate: String) => {
                      securityPriceRead
                        .lookupSecurityItemAndPriceByIsinAndPeriodEndDate(cNames, periodEndDate)
                    },
                    workInvDataGUESSINGSOURCE
                  )
                  outputParams = strategy.getOutputParams(selection)
                case None => BadRequest("can not find fund admin type")
              }
          }
          SafeLogger.logString(Logger, "begin pivot table:" + TimeService.currentTime.toString)
          PivotTableFill.generateAllPivot(outputParams, workbook, currentFundAuditPeriodEnd)
          SafeLogger.logString(
            Logger,
            "END EXPORT AND WRITE TO DATABASE: " + TimeService.currentTime.toString
          )
          val xworkbook = new SXSSFWorkbook(workbook, 1000)
          try {
            xworkbook.write(bstream)
          } finally {
            if (xworkbook != null) {
              xworkbook.close()
            }
          }
          //workbook.write(bstream)
        } catch {
          case e: Exception => {
            SafeLogger.logString(Logger, e.getMessage)
            throw e
          }
        } finally {
          if (workbook != null) {
            workbook.close()
          }

        }
        SafeLogger.logString(Logger, "PREPARING BLOB: " + TimeService.currentTime.toString)
        val generatedFileContent = new SerialBlob(bstream.toByteArray)
        SafeLogger.logString(Logger, "BLOB: READY:" + TimeService.currentTime.toString)
        val fileFundName = if (currentFundName.length > 200) {
          currentFundName.substring(0, 200)
        } else {
          currentFundName
        }
        fundAuditHistoryWrite
          .create(
            FundAuditHistory(
              randomUUID().toString,
              fundEngagementId,
              0,
              Some(generatedFileContent),
              Some("Single EGA - " + fileFundName + ".xlsx"),
              GenerateStatus.success.toString,
              comment,
              Some(userEmail),
              Some(TimeService.currentTime)
            )
          )
          .map(res => {
            SafeLogger
              .logString(Logger, "END WRITE TO DATABASE: " + TimeService.currentTime.toString)
          })
      })
      .map(_ => Ok)
  }
  }