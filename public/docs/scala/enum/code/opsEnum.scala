object Ops extends Enumeration {
  type Ops = Value
  val Update, Read, Delete, Remove, Unknown = Value

  implicit def fromString(op: String): Ops =
    values.find(_.toString.toLowerCase == op.toLowerCase()).getOrElse(Unknown)

  implicit def toStr(op: Ops): String = op.toString.toLowerCase
}
