def runNamedProcess(paras : List[Object], clazzName:String, methodName:String, prioritySet: Option[Int] = None,name:String =singleDBJob) ={
  val msg = ProcessCallMsg(clazzName,methodName,paras.map{_.asInstanceOf[AnyRef]}.toArray)
  val jobMsg = JobMsg("SimpleProcess",msg)
  BackendServer.runNamedProcessCommand(jobMsg,name,priority = prioritySet)
}