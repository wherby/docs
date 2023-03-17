import java.sql.Date
import com.pwc.ds.awm.const.ExtractionWarningCode
import com.pwc.ds.awm.processor.BaseReportProcessor
import com.pwc.ds.awm.processor.exceptions.ExtractionWarning
import com.pwc.ds.awm.processor.generateEGA.basicReport.{GeneralSingleEGAStorage, MorgenSingleEGAStorage, WarningStatus}
import com.pwc.ds.awm.processor.morgan.excelreader.MorganExcelPSRReader
import com.pwc.ds.awm.processor.singleegarow.PSRRow.TradeDate

class MorganPSRProcessor extends BaseReportProcessor {
  override def extractToObjectForPeriod(sourceFilePath: String, frontEndFundName: String, frontEndPeriodStart: Date, frontEndPeriodEnd: Date): GeneralSingleEGAStorage = {
    val storage = new MorgenSingleEGAStorage()
    val psrTable = MorganExcelPSRReader.readPSRSheet(sourceFilePath, frontEndFundName)
    val (row, rowAE, _) = GeneralSingleEGAStorage.splitTimeRange(psrTable, TradeDate, frontEndPeriodStart, frontEndPeriodEnd)
    storage.purchaseAndSaleTransactionReportPSRData = row
    storage.purchaseAndSaleTransactionReportPSRAfterYEData = rowAE
    if(row.length != 0 && rowAE.length !=0){
      //throw  ExtractionWarning(ExtractionWarningCode.AFTER_YE_DATA)
      storage.warningMsg=WarningStatus(true,s"PSR and PSR (after YE)")
    }
    storage
  }
}
