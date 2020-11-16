def verifyExcelFolder()=Action{
  val folferPath ="../temp/"
  val files = recursiveListFiles(new File(folferPath))
  val excelFiles =files.filter(file =>file.getName.toLowerCase.endsWith(".xls")||
    file.getName.toLowerCase.endsWith(".xlsx"))
  val fileInfos = excelFiles.map{
    file => new FileInfo(file.getName,file.getAbsolutePath)
  }
  val resultsFuture = Future.sequence( fileInfos.toSeq.map{
    fileInfo => Future( verifyFileForNoMatchedFormula(fileInfo))
  })
  Ok(files.length.toString)
}
