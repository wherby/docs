import java.sql.Timestamp

override def checkPermissionThenGetFiDataAndNotification(dataFrontEnd: FinancialdataGetFromFrontend,
                                                         email: String,
                                                         privilege: String,
                                                         companyuser: Option[Companyuser]) = {
  val currentFinancialData: Financialdata = companyfinancialdataServiceRead.waitForLookupWithDefaultValues(dataFrontEnd.companyId, dataFrontEnd.year) match {
    case Some(data) => data
    case None => throw new Exception("Financial data doesn't exist")
  }
  FinancialDataWorkflowUtils.checkRolePermissionThenGetFiDataAndNotification(dataFrontEnd, currentFinancialData, email, privilege, companyuser)
}

def checkRolePermissionThenGetFiDataAndNotification(dataFrontEnd:FinancialdataGetFromFrontend,
                                                    currentFinancialData:Financialdata,
                                                    user:String,
                                                    privilege:String,
                                                    companyuser:Option[Companyuser]):NextFinancialDataAndNotification= {
  val now = TimeService.currentTime
  val companyuserName = companyuser.map(_.name).getOrElse("")
  val workflowState = WorkflowState(currentFinancialData.internalState, currentFinancialData.externalState,currentFinancialData.modifiedByExternal, currentFinancialData.uploadFilesByExternal)
  OperationTypes.withName(dataFrontEnd.operationType.toUpperCase) match {
    case OperationTypes.external_data_save =>      externalSave(dataFrontEnd,currentFinancialData,user, privilege, now,companyuserName)
    case OperationTypes.external_file_save =>      externalFileSave(dataFrontEnd,currentFinancialData,user, privilege, now,companyuserName)
    case OperationTypes.external_data_finalize =>  externalFinalize(dataFrontEnd,currentFinancialData,user, privilege, now,companyuserName)
    case OperationTypes.external_data_approve =>   externalApprove(dataFrontEnd,currentFinancialData,user, privilege, now,companyuserName)
    case OperationTypes.file_save =>               internalFileSave(dataFrontEnd,currentFinancialData,user, privilege, now,companyuserName)
    case OperationTypes.data_save => {
      if(currentFinancialData.modifiedByExternal.getOrElse(false) && !StatusHelper.isInternalApproved(workflowState) && !( workflowState.externalState.getOrElse(None) == ExternalState.ApprovedOnlyFiles))
        sentBack(dataFrontEnd,currentFinancialData,user, privilege, now,companyuserName)
      else
        internalSave(dataFrontEnd,currentFinancialData,user, privilege, now,companyuserName)
    }
    case OperationTypes.external_data_agree =>               agree(dataFrontEnd,currentFinancialData,user, privilege, now,companyuserName)
    case OperationTypes.external_data_disagree =>         disagree(dataFrontEnd,currentFinancialData,user, privilege, now,companyuserName)
    case OperationTypes.data_finalize =>          internalFinalize(dataFrontEnd,currentFinancialData,user, privilege, now,companyuserName)
    case OperationTypes.data_signOff =>            internalSignOff(dataFrontEnd,currentFinancialData,user, privilege, now,companyuserName)
    case OperationTypes.data_approve =>            internalApprove(dataFrontEnd,currentFinancialData,user, privilege, now,companyuserName)
    case _ => throw UnAuthorizationException("Operation type doesn't exists")
  }
}

def externalSave(dataFrontEnd:FinancialdataGetFromFrontend,
                 currentFinancialData:
                 Financialdata,
                 user:String,
                 privilege:String,
                 now: Timestamp,
                 companyuserName:String) = {
  val workflowState = WorkflowState(currentFinancialData.internalState,currentFinancialData.externalState,currentFinancialData.modifiedByExternal, currentFinancialData.uploadFilesByExternal)
  var modifiedByExternal = getModifiedByExternal(dataFrontEnd, currentFinancialData)

  if(actionsValidator.validateExternalSave(privilege,workflowState))
  {
    val newfdata = currentFinancialData.copy(currencyId = dataFrontEnd.currencyId,assetdata = dataFrontEnd.assetdata,
      liabilitydata = dataFrontEnd.liabilitydata, profitlossdata = dataFrontEnd.profitlossdata,
      fmvdata = dataFrontEnd.fmvdata, ordinaryearningsandgain = dataFrontEnd.ordinaryearningsandgain,

      modifiedByExternal = modifiedByExternal)
    val noti = Nil
    NextFinancialDataAndNotification(newfdata,noti)
  }
  else
    throw UnAuthorizationException()
}


