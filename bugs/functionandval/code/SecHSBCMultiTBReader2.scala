import com.pwc.ds.awm.processor.pdfExtractorMethods.HSBCMFLikeExtractor
import com.pwc.ds.awm.processor.secHSBCMulti.reader.SecHSBCMultiTBReader.readerConfig
import com.pwc.ds.awm.processor.singleegarow.TBRow.{AccountName, Balance, ClosingBalance_Detail, Description_Detail, Group1, createTBRowMap}

object SecHSBCMultiTBReader2 {
  val removeStr =
    Seq(
      "HSBC         Securities             Services",
      "Equity,     Income       and    Expense        Report",
      "Fund code                 Valuation  date          Ledger  description/Securities  Ledger group  description/ Ledger  code                 Balance  in base             %  of  NAV Client  ID"
    )

  val splitPoints = Seq(26, 51, 102, 139, 168).map(x => x + 4)
  val readerConfig = HSBCMFLikeExtractor(
    removeStr,
    splitPoints,
    Some(4),
    Some(HSBCMFLikeExtractor.splitTableToRecords),
    splitSeq = Seq((2, splitString))
  )
  val splitWords = Seq(
    "EQUITY",
    "EXPENSES",
    "DISTRIBUTION",
    "UNREALISED",
    "RESERVES",
    "INVESTMENT",
    "SUBSCRIPTION",
    "DISTRIBUTABLE",
    "REALISED",
    "ACCUMULATED"
  )

  def splitString = (str: String) => {
    try {
      val wordFind = splitWords.filter(word => str.indexOf(word) > 0).head
      val index    = str.lastIndexOf(wordFind)
      Seq(str.substring(0, index), str.substring(index, str.length))
    } catch {
      case _: Throwable =>
        //println("*" * 100 + " " + str)
        Seq(str, "   ")
    }
  }

  def getRecord(filePath: String) = {
    val res = HSBCMFLikeExtractor.extractRecordsFromFile(readerConfig, filePath)
    res.map(row => getRow(row))
  }

  def getRow(row: Seq[String]): Map[String, String] = {
    (
      createTBRowMap() ++ Map(
        Group1                -> row(0),
        Description_Detail    -> row(2),
        ClosingBalance_Detail -> row(5),
        AccountName           -> row(3),
        Balance               -> row(5)
      )
      ).toMap
  }
}