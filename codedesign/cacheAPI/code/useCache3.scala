import java.util.concurrent.TimeUnit

object AppCache {

  //http://cloudtu.github.io/blog/2018/08/guava-cache-memo.html
  lazy val resultCache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build[String, AnyRef]


  def putValue(key: String, value: AnyRef) = {
    resultCache.put(key, value)
  }

  def getValue(key: String) = {
    val res = resultCache.getIfPresent(key)
    if (res == null) {
      None
    } else {
      Some(res)
    }
  }


  private def getOrSetCacheToSpecifiedType[T<:AnyRef](key:String, fx: () => AnyRef)={
    val cacheValueOpt = getValue(key)
    val resOpt = try {
      cacheValueOpt match {
        case Some(value) => Some(value.asInstanceOf[T])
        case _ =>
          val res = fx()
          val resV = res.asInstanceOf[T]
          putValue(key, resV)
          Some(resV)
      }
    } catch {
      case _: Throwable => None
    }
    resOpt
  }

  def getTabValue(key: String, fx: () => AnyRef) = {
    val resOpt =getOrSetCacheToSpecifiedType[Seq[Seq[ExcelField]]](key,fx)
    resOpt.getOrElse(Seq[Seq[ExcelField]]())
  }

  def getTabValueList(key: String, fx: () => AnyRef) = {
    val resOpt = getOrSetCacheToSpecifiedType[Seq[Seq[Seq[ExcelField]]]](key,fx)
    resOpt.getOrElse(Seq[Seq[Seq[ExcelField]]]())
  }
}