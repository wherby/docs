import java.util.concurrent.TimeUnit

object AppCache {

  //http://cloudtu.github.io/blog/2018/08/guava-cache-memo.html
  lazy val resultCache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build[String, AnyRef]



  def putValue(key:String,value:AnyRef)={
    resultCache.put(key,value)
  }

  def getValue(key:String) ={
    resultCache.getIfPresent(key)
  }

  private def getTabValueListFromCache(key:String)={
    try{
      val res = getValue(key)
      if(res == null){
        None
      }else{
        Some(res.asInstanceOf[Seq[Seq[Seq[ExcelField]]]])
      }
    }catch {
      case _:Throwable =>None
    }
  }

  private def getTabValueFromCache(key:String)={
    try{
      val res = getValue(key)
      if(res == null){
        None
      }else{
        Some(res.asInstanceOf[Seq[Seq[ExcelField]]])
      }
    }catch {
      case _:Throwable => None
    }
  }

  def getTabValue(key:String,fx:()=>AnyRef)={
    val cacheValue = getTabValueFromCache(key)
    val resOpt= cacheValue match {
      case Some(value) => cacheValue
      case _=>
        try{
          val res = fx()
          val resV= res.asInstanceOf[Seq[Seq[ExcelField]]]
          putValue(key,resV)
          Some(resV)
        }catch {
          case _:Throwable => None
        }
    }
    resOpt.getOrElse(Seq[Seq[ExcelField]]())
  }

  def getTabValueList(key:String,fx:()=>AnyRef)={
    val cacheValue = getTabValueListFromCache(key)
    val resOpt= cacheValue match {
      case Some(value) => cacheValue
      case _=>
        try{
          val res = fx()
          val resV= res.asInstanceOf[Seq[Seq[Seq[ExcelField]]]]
          putValue(key,resV)
          Some(resV)
        }catch {
          case _:Throwable =>
            None
        }
    }
    resOpt.getOrElse(Seq[Seq[Seq[ExcelField]]]())
  }
}