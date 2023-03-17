def runProcess(paras : List[AnyRef], clazzName:String, methodName:String, prioritySet: Option[Int] = None) ={
  val setTimeOut:Timeout = 6000 seconds
  val msg = ProcessCallMsg(clazzName,methodName,paras.map{_.asInstanceOf[AnyRef]}.toArray)
  val jobMsg = JobMsg("SimpleProcessFuture",msg)
  val namedSevice = Seq("com.pwc.ds.cidr.project.creditreview.processors.OcrPlusProcessor")
  if(namedSevice.contains(clazzName)){
    BackendServer.runNamedProcessCommand(jobMsg,OCRJOBStr, priority = prioritySet,timeout = setTimeOut).map{
      jobResult=>
        if((jobResult.result.asInstanceOf[ProcessResult]).jobStatus.toString == "Failed" ){
          throw new RuntimeException(jobResult.result.asInstanceOf[ProcessResult].result.asInstanceOf[Exception].getMessage)
        }
        jobResult.result.asInstanceOf[ProcessResult].result.asInstanceOf[ProcessorResultValue]
    }
  }else{
    BackendServer.runProcessCommand(jobMsg,priority = prioritySet,timeout = setTimeOut).map{
      jobResult=>
        if((jobResult.result.asInstanceOf[ProcessResult]).jobStatus.toString == "Failed" ){
          throw new RuntimeException(jobResult.result.asInstanceOf[ProcessResult].result.asInstanceOf[Exception].getMessage)
        }
        jobResult.result.asInstanceOf[ProcessResult].result.asInstanceOf[ProcessorResultValue]
    }
  }
}