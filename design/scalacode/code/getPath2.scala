  def getParentPath(path:String):Future[Seq[String]]= {
    var ret:Seq[String] = Seq()
    if(path.isEmpty){
      Future(ret)
    }else{
      val recordFutureOpt = recordDAO.getById(path)

      val result =  recordFutureOpt.flatMap {
        recordOpt =>recordOpt.map{
          record => val inheritedOpt = record.etcs.inheritFrom
            inheritedOpt match {
              case Some(inheritedPath)=>ret = ret:+inheritedPath
                          getParentPath(inheritedPath).map{
                            seq=>ret ++seq
                          }
              case _=>path.lastIndexOf(".") match {
                case -1 =>  Future(ret)
                case idx => val ppath = path.substring(0,idx)
                  ret = ret :+ppath
                getParentPath(ppath).map(seq =>ret ++seq)
              }
            }
        }.getOrElse(Future(ret))
      }
      result
    }