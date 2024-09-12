  def getParentPath(path:String):Seq[String]={
    var ret:Seq[String] = Seq()
    if(path.isEmpty){
      ret
    }else{
      val recordFutureOpt = recordDAO.getById(path)

      val result =  recordFutureOpt.map {
        recordOpt =>recordOpt.map{
          record => val inheritedOpt = record.etcs.inheritFrom
            inheritedOpt match {
              case Some(inheritedPath)=>ret = ret:+inheritedPath
                          ret ++=getParentPath(inheritedPath)
              case _=>path.lastIndexOf(".") match {
                case -1 =>  ret
                case idx => val ppath = path.substring(0,idx)
                  ret = ret :+ppath
                ret =ret ++getParentPath(ppath)
              }
            }
        }.getOrElse(Future(ret))
      }
      Await.result( result,1.second)
      ret
    }
  }