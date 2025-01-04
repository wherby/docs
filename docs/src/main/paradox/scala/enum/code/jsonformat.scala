
...
  implicit object opsW extends Writes[Ops] {
    override def writes(o: Ops): JsString = {
      JsString(Ops.toStr(o))
    }
  }
  implicit object opsReads extends Reads[Ops] {
    override def reads(json: JsValue): JsResult[Ops] = {
      json match {
        case jsString: JsString => JsSuccess(Ops.fromString(jsString.toString()))
        case s                  => JsError(s"Invalid value: $s")
      }
    }
  }
...