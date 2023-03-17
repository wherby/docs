class DatabaseUpdateProcessor {
  def exportConfirmationToDatabase(user: String, fileName: String, ***)={
    Await.result( FundEngagementReportTypeSelectionController.exportConfirmationToDatabase(user,fileName,***), 10 seconds)
  }
}
