import java.io.{ByteArrayOutputStream, FileInputStream}
import java.nio.file.{Files, Paths}
import java.util.Calendar
import java.util.UUID.randomUUID
import javax.sql.rowset.serial.SerialBlob
import scala.concurrent.{Await, Future, duration}
import scala.concurrent.duration.Duration

def uploadOriginalFile(year: Int, companyId: String, uploaderType: String, operationType: String) = Action.andThen(ipRateLimitFilterPerDay).async(parse.multipartFormData(maxLength = 1024 * 1024 * 1)) { implicit request =>
  ...
  val financialData = Await.result(companyfinancialdataServiceRead.lookup(companyId, year), 10 second)

  var financialDataFromFrontEnd: FinancialdataGetFromFrontend = FinancialdataGetFromFrontend.financialDataToFrontEnd(financialData.get, operationType)
  var companyUser = companyuserServiceRead.waitForLookup(companyId)
  /**
   * Financial Data Ratio.
   */
  val workbook = WorkbookFactory.create(filetest)
  val financialDataSource = FinancialDataUtils.extractFinancialDataFromFile(workbook)

}.getOrElse(Future(BadRequest("Unable to Read File"))) val fdSourceStr = Json.toJson(financialDataSource).toString()

...
companyNameCell.setCellValue(companyName)
val yearCell = sheet.getRow(3).getCell(1)
yearCell.setCellValue(yearStr)
val bstream = new ByteArrayOutputStream()
ratioWB.write(bstream)
val ratioFileContent = new SerialBlob(bstream.toByteArray)
/**
 * End of Financial Ratio.
 */

var inputStream = new FileInputStream(filetest)
var byteArray = FileUtils.inputStreamToByte(inputStream)
var fileContentId = randomUUID().toString
var fileContent = new OriginalFinancialFileContent(fileContentId, file.filename, new javax.sql.rowset.serial.SerialBlob(byteArray), Some(fdSourceStr), Some(ratioResultStr), Some(ratioFileContent))
var fileRecord = new OriginalFinancialFile(randomUUID().toString, "", financialDataFromFrontEnd.year, financialDataFromFrontEnd.companyId.toString, file.filename, fileContentId, true, emailOption, Some(TimeService.currentTime))

}
