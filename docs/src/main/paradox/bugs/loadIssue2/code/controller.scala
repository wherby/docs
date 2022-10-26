
def lookupEngagementsCompletion() = deadbolt.SubjectPresent()(parse.anyContent) {
  implicit authRequest =>
  {
    val engagementIdsJsArray = authRequest.body.asJson
    engagementIdsJsArray match {
      case Some(array) => {
        val engagementIds = array.as[Seq[String]]
        getCompletionRatioByEngagementIds(engagementIds).map(res => {
          Ok(Json.toJson(res))
        })
      }
      case _ => {
        val emptyMap: Map[String, Double] = Map()
        Future(Ok(Json.toJson(emptyMap)))
      }
    }
  }
}

def getCompletionRatioByEngagementIds(engagementIds: Seq[String]) = {
  val engagementIdRatioMap = engagementIds.map(engagementId => {
    val result = for {
      fundEngagements <- logTime("") { fundEngagementRead.queryByEngagmentId(engagementId) }
      reportRatios    <- logTime("") { getReportCompletionRatioByEngagementId(engagementId) }
    } yield {
      val fundEngagementIds = fundEngagements.map(_.id)
      for {
        egaData <- logTime("") { egaRead.lookupByFundEngagementIds(fundEngagementIds) }
        feId_UploadAndAccountNamesMap <-
          logTime("") {
            fundEngagementReportTypeSelectionRead
              .getUploadConfirmationDividedByDataFormat_WithAccountNameList(fundEngagementIds)
          }
        //confirmationRatios <- getConfirmationCompletionRatioByEngagement(engagementId)
        fundAccountsWithFEId <- logTime("") {
          fundAccountRead.lookupByFundEngagementIds(fundEngagementIds)
        }
        singleEGARatios <- logTime("") {
          getSingleEGACompletionRatioByEngagementId(fundEngagementIds)
        } //ok
      } yield {
        logTime("TEST: CALCULATE RATIOS") {
          val feId_fundId = reportRatios.map(r => (r.fundEngagementId, r.fundId))
          val confirmationRatios = getConfirmationCompletionRatioByFundEngagementIds(
            fundEngagementIds,
            fundAccountsWithFEId,
            feId_fundId,
            feId_UploadAndAccountNamesMap
          )
          val valuationRatios = getValuationCompletionRatioByEngagementId(egaData) //ok
          val fundEngagementsRatios = fundEngagementIds
            .map(fundEngagementId => {
              val maybeReportRatio =
                reportRatios.filter(_.fundEngagementId == fundEngagementId).headOption
              val maybeConfirmationRatio =
                confirmationRatios.get(fundEngagementId)
              val maybeValuationRatio = valuationRatios.get(fundEngagementId)
              val maybeSingleEGARatio = singleEGARatios.get(fundEngagementId)
              val reportRatio = maybeReportRatio match {
                case Some(value) => {
                  if (value.definedReportTypeNumber > 0) {
                    0.5 * value.reportUploadedNumber / value.definedReportTypeNumber
                  } else {
                    0
                  }
                }
                case _ => 0
              }
              val confirmationRatio = maybeConfirmationRatio match {
                case Some(value) => {
                  if (value.total > 0) {
                    0.17 * value.finished / value.total
                  } else {
                    0
                  }
                }
                case _ => 0
              }
              val valuationRatio = maybeValuationRatio match {
                case Some(value) => {
                  if (value._1 == true && value._2 == true) {
                    0.16
                  } else if (value._1 == false && value._2 == false) {
                    0
                  } else {
                    0.08
                  }
                }
                case _ => 0
              }
              val singleEGARatio = maybeSingleEGARatio match {
                case Some(value) => {
                  if (value > 0) {
                    0.17
                  } else {
                    0
                  }
                }
                case _ => 0
              }
              reportRatio + confirmationRatio + valuationRatio + singleEGARatio
            })
          val engagementRatio = if (fundEngagementsRatios.length > 0) {
            fundEngagementsRatios.reduce(_ + _) / fundEngagementsRatios.length
          } else {
            0
          }
          (engagementId, (engagementRatio, fundEngagementsRatios.length))
        }
      }
    }
    result.flatten
  })
  Future.sequence(engagementIdRatioMap).map(_.toMap)
}
