import java.io.{File, FileInputStream}

object AppFileIO {
  def fileToWorkBook(filePath: String) = {
    WorkbookFactory.create(new FileInputStream(new File(filePath)))
  }