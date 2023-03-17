import scala.concurrent.Future

/** @param fundEngagementId
 * @return
 */
override def getUploadConfirmationDividedByDataFormat_WithAccountNameList(
                                                                           fundEngagementIds: Seq[String]
                                                                         ): Future[
  Map[
    String,
    (
      Seq[FundEngagementReportTypeSelectionData],
        Seq[FundEngagementReportTypeSelectionData],
        Seq[String]
      )
  ]
] = {
  for {
    selectionListAll <- lookupByFundEngagementIds(fundEngagementIds)
    confirmationSheettypesReporttypesMapIds <- ehCacheService
      .getConfirmationSheettypesReporttypesMapId()
    (oldTypeIds, newTypeIds)              <- ehCacheService.getSheettypesReporttypesMapId_ByOldAndNew()
    positionCashSheettypeReporttypeMapIds <- ehCacheService.lookupPositionCashSheetTypeIds()
  } yield {
    fundEngagementIds
      .map(id => {
        var selectionList = selectionListAll.filter(selection => selection.fundEngagementId == id)
        var dataWithOldType = selectionList.filter(selection => {
          oldTypeIds.contains(
            selection.sheettypesReporttypesMapId
          ) && selection.fundEngagementId == id
        })
        var dataWithNewType = selectionList.filter(selection => {
          newTypeIds.contains(
            selection.sheettypesReporttypesMapId
          ) && selection.fundEngagementId == id
        })
        val positionCashIDs          = positionCashSheettypeReporttypeMapIds
        var availableAccountNameList = getAccountNameList(selectionList, positionCashIDs)
        (id, (dataWithOldType, dataWithNewType, availableAccountNameList))
      })
      .toMap
  }
}