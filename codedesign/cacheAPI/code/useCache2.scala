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


  def getTabValue(key: String, fx: () => AnyRef) = {
    val cacheValue = getValue(key)
    val resOpt = try {
      cacheValue match {
        case Some(value) => Some(value.asInstanceOf[Seq[Seq[ExcelField]]])
        case _ =>
          val res = fx()
          val resV = res.asInstanceOf[Seq[Seq[ExcelField]]]
          putValue(key, resV)
          Some(resV)
      }
    } catch {
      case _: Throwable => None
    }
    resOpt.getOrElse(Seq[Seq[ExcelField]]())
  }

  def getTabValueList(key: String, fx: () => AnyRef) = {
    val cacheValue = getValue(key)
    val resOpt = try {
      cacheValue match {
        case Some(value) => Some(value.asInstanceOf[Seq[Seq[Seq[ExcelField]]]])
        case _ =>
          val res = fx()
          val resV = res.asInstanceOf[Seq[Seq[Seq[ExcelField]]]]
          putValue(key, resV)
          Some(resV)
      }
    } catch {
      case _: Throwable =>
        None
    }
    resOpt.getOrElse(Seq[Seq[Seq[ExcelField]]]())
  }
}