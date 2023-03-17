
def readPDF(sourceFile:String)={
  extractAllTextFromPdfFile(sourceFile,EmptyConfigForPDF.extractorConfig)
}

def extractAllTextFromPdfFile(sourceFilePath: String, reportConfig: ExtractorConfig): String = {
  try {
    var content = ""
    if (isPdfFileExtension(sourceFilePath)) {
      var file = new File(sourceFilePath);
      val document = PDDocument.load(file);
      var stripper:LayoutPdfTextStripper = new LayoutPdfTextStripper;
      stripper.setReportConfig(reportConfig)
      stripper.setSortByPosition(true);
      content = stripper.getText(document);
      document.close()
    }
    content
  } catch {
    case e: IOException => throw new IOException("read source pdf file IO error")
    case e: Exception => throw new Exception(JsError("read source pdf file IO error"))
  }
}