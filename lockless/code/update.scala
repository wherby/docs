
def update(file:String,dbid:String)={
  val files = dbreader.getfiles(dbid)
  dbwrite.record(files.append(file))
}