import java.security.MessageDigest
val a ="test string"

val b = MessageDigest.getInstance("MD5").digest(a.getBytes).toString
val c = MessageDigest.getInstance("MD5").digest(a.getBytes).toString
b == c

val e = MessageDigest.getInstance("MD5").digest(a.getBytes).mkString
val f = MessageDigest.getInstance("MD5").digest(a.getBytes).mkString

e == f



