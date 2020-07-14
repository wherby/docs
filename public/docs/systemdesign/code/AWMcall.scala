def runProcess(paras : List[Object], clazzName:String, methodName:String, prioritySet: Option[Int] = None) ={
  println(clazzName)
  val msg = ProcessCallMsg(clazzName,methodName,paras.map{_.asInstanceOf[AnyRef]}.toArray)
  val jobMsg = JobMsg("SimpleProcess",msg)
  BackendServer.runProcessCommand(jobMsg,priority = prioritySet)
}