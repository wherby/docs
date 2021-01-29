object GenericConfirmationFinder {
  def getConfirmationRecord(reportId: String,
                            filterFunc: (ConfirmationRecord) => Boolean,
                            selections: Seq[FundEngagementReportTypeSelectionData]): Option[ConfirmationRecordWithFile] = {
    val reportOpt = selections.filter(select => select.sheettypesReporttypesMapId == reportId && select.extractionFileContent.isDefined).headOption
    val allRecords = reportOpt.flatMap {
      report =>
        report.extractionFileContent.map {
          extractStr =>
            ConfirmationStorageHelper.stringToStorage(extractStr).files.flatMap {
              file => file.records.map(record => ConfirmationRecordWithFile(record, file.fileName))
            }
        }
    }.getOrElse(Seq())
    allRecords.filter(record => filterFunc(record.record)).headOption
  }

  def queryInfo(reportId: String, filterFunc: (ConfirmationRecord) => Boolean, getFunc: ConfirmationRecordWithFile => Map[String, String], selections: Seq[FundEngagementReportTypeSelectionData]): Map[String, String] = {
    getConfirmationRecord(reportId, filterFunc, selections).map {
      record => getFunc(record)
    }.getOrElse(Map())
  }

  def getWorkCash(workCash: Seq[Map[String, String]], selections: Seq[FundEngagementReportTypeSelectionData], fundFinderConfig: FundFinderConfig) = {
    workCash.map {
      workCashRow =>
        val glAccountName = workCashRow.get(WorkCashColumns.GL_ACCOUNT_NAME).getOrElse("")
        if (fundFinderConfig.belongsToFund(glAccountName)) {
          var queriedResult: Map[String, String] = Map()
          fundFinderConfig.workCashConfs.map {
            findConfig => queriedResult = queriedResult ++ queryInfo(findConfig.reportId, findConfig.filterFunc(workCashRow), findConfig.getFunc, selections)
          }
          workCashRow ++ queriedResult
        } else {
          workCashRow
        }
    }
  }

  def getWorkInv(workInv: Seq[Map[String, String]], selections: Seq[FundEngagementReportTypeSelectionData], fundFinderConfig: FundFinderConfig) = {
    workInv.map {
      workInvRow =>
        val glAccountName = workInvRow.get(WorkInvColumns.CUSTODIAN).getOrElse("")
        if (fundFinderConfig.belongsToFund(glAccountName)) {
          var queriedResult: Map[String, String] = Map()
          fundFinderConfig.workInvConfs.map {
            findConfig => queriedResult = queriedResult ++ queryInfo(findConfig.reportId, findConfig.filterFunc(workInvRow), findConfig.getFunc, selections)
          }
          workInvRow ++ queriedResult
        } else {
          workInvRow
        }
    }
  }

}

case class FinderConfig(reportId: String, filterFunc: Map[String, String] => ConfirmationRecord => Boolean, getFunc: ConfirmationRecordWithFile => Map[String, String])

case class FundFinderConfig(belongsToFund: (String) => Boolean, workCashConfs: Seq[FinderConfig], workInvConfs: Seq[FinderConfig])
