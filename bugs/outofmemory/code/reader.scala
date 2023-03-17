import java.io.{File, FileInputStream}

object AppFileIO {
  def fileToWorkBook(filePath: String) = {
    WorkbookFactory.create(new File(filePath))
  }

  def fileToStream(filePath:String)={
    val is = new FileInputStream(new File(filePath))
    val workbook = StreamingReader.builder().rowCacheSize(1000) // number of rows to keep in memory
      .bufferSize(4096) // index of sheet to use (defaults to 0)
      .open(is); // InputStream or File for XLSX file (required)
    workbook
  }
}