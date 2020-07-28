def handleConfirmationsResult(user: String, fileName: String, ***) = {
  if (jobResult.result.isInstanceOf[ProcessResult]) {
    val storageResult = jobResult.result.asInstanceOf[ProcessResult]
    if (storageResult.result.isInstanceOf[ConfirmationSingleEGAStorage]) {
      //Original code
      //Await.result(FundEngagementReportTypeSelectionController.exportConfirmationToDatabase(user, fileName,***), 10 seconds)

      //Using namedjob
      doraServer.runNamedProcess(List(user,fileName,***),"com.pwc.ds.awm.processor.DatabaseUpdateProcessor","exportConfirmationToDatabase")
    }
  }
}