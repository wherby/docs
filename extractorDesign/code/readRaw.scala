def readRawPDF(sourceFile:String):String={
  val document = PDDocument.load(new File(sourceFile))
  val striper = new PDFTextStripper()
  striper.setSortByPosition(true)
  val content = striper.getText(document)
  document.close()
  content
}