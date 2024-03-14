package com.pwc.ds.cidr.event

import akka.actor.ActorSystem
import com.pwc.ds.cidr.client.{FileOperator}
import com.pwc.ds.cidr.db.{VendorTaskDao, VendorTaskResultDao, VendorTaskResultDaoCreateIn}
import com.pwc.ds.cidr.engine.api.JobFactory
import com.pwc.ds.cidr.engine.core.{JobRunner, JobRunnerImpl}
import com.pwc.ds.cidr.engine.io.{FileEngine, FileValue}
import com.pwc.ds.cidr.event.VendorBusinessUtils.{ExtractKey, NumberValueTitleRegex}
import com.pwc.ds.cidr.event.VendorBusinessUtils.TitleCell.{TitleCellKey, TitleCellValue}
import com.pwc.ds.cidr.event.VendorCommon.Key
import com.pwc.ds.cidr.project.creditreview.processors.OcrResultAndPageInfoJasonValues.DocumentInfo
import com.pwc.ds.cidr.project.creditreview.processors.{DocumentDTO, DocumentInfoDTO}
import org.apache.poi.ss.usermodel.{CellType, Sheet, WorkbookFactory}
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.read
import org.json4s.{Formats, NoTypeHints}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.json.{Format, Json}
import slick.jdbc.JdbcBackend.Database

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.Timestamp
import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.BufferedSource
import scala.util.{Random, Try}

class VendorTaskPreLogConsumer @Inject()(vendorTaskDao: VendorTaskDao,
                                         vendorTaskResultDao: VendorTaskResultDao,
                                         db: Database,
                                         config: Configuration,
                                        )(implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {

  import profile.api._

  private def adapter(taskRow: VendorTaskRow) = AuditLogRow(taskRow.key, taskRow.status, -1, taskRow.uuid, taskRow.name, taskRow.createBy, Some(taskRow.location), createAt = Some(taskRow.createAt))

  override protected def auditLogRowGenerator(offset: Long, batchSize: Int): Future[Seq[AuditLogRow]] = {
    val sql = VendorTask.filter(_.key > offset).filter(_.isdeleted === false)
      .take(batchSize).result
    Future.successful(Await.result(db.run(sql), 2.seconds).map(adapter))
      .recover[Seq[AuditLogRow]] {
        case _: Throwable => Seq.empty
      }
  }

  override def logFilter: AuditLogRow => Boolean = row => row.event == VendorTaskDao.Status.CREATED || row.event == VendorTaskDao.Status.VENDOR_PREPROCESSING

  override def businessName: String = "log_committer_vendor_task_preprocess"

  override def processor: Processor = (_: Option[AuditLogRow], cur: AuditLogRow) => {
    implicit val key: Key = Key(cur.id)
    val status = cur.event
    val id = cur.entityId // uuid
    val name = cur.entityType
    val location = cur.message.getOrElse("error dsri")
    try {
      status match {
        case VendorTaskDao.Status.CREATED =>
          Await.result(vendorTaskDao.updateStatus(id, VendorTaskDao.Status.VENDOR_PREPROCESSING), 2.seconds)
        case _ =>
      }
      val fileValue = FileValue(location, name)
      val response = Await.result(Future.successful(submit(VendorCommon.workingDir, fileValue)), 10.minutes)
      Await.result(vendorTaskResultDao.create(VendorTaskResultDaoCreateIn(id, s"${name.split("\\.").headOption.getOrElse(s"$name-e")}-document.json", response.dsri, VendorTaskResultDaoCreateIn.Type.DOCUMENT)), 2.seconds)
      Await.result(vendorTaskDao.updateStatus(id, VendorTaskDao.Status.VENDOR_PREPROCESSED), 2.seconds)
      Await.result(vendorTaskDao.updateExpireAt(id, new Timestamp(org.joda.time.DateTime.now().plusDays(1).toDate.getTime)), 2.seconds)
    } catch {
      case ex: Throwable =>
        LoggerFactory.getLogger(this.getClass).error("vendor preprocess failed: " + ex.getMessage + " --- " + ex.getStackTrace.mkString("---"))
        try Await.result(vendorTaskDao.updateStatus(id, VendorTaskDao.Status.VENDOR_PREPROCESS_FAILED), 2.seconds)
        catch {
          case _: Throwable => LoggerFactory.getLogger(this.getClass).error("database timeout Exception" + ex.getMessage + " --- " + ex.getStackTrace.mkString("---"))
        }
    }
    Future.successful(true)
  }

  val jobRunner: JobRunner = new JobRunnerImpl

  def submit(workingDir: String, fileValue: FileValue): FileValue = {

    val engagementSetting = Map("engagementSetting" -> Map(
      "ocr" -> Map("method" -> "paoding")
    ))
    val documentJob = JobFactory.makeDocumentOutputJob(DocumentDTO(id = 1, fileList = Seq(fileValue), documentType = "any", metaData = Some(Json.stringify(Json.toJson(engagementSetting)))))
    Await.result(jobRunner.doAJob(documentJob, workingDir)
      .map(output => {
        val documentInfoObj = output("last").asInstanceOf[DocumentInfoDTO]
        //        val documentInfo: DocumentInfo = read[DocumentInfo](scala.io.Source.fromFile(documentInfoObj.filepath).mkString)
        //        Json.fromJson[DocumentInfo](Json.parse(documentInfoObj.filepath))(Json.reads)
        documentInfoObj.file
      }), 10.minutes)
  }
}

object VendorCommon {
  case class Key(data: Long)

  def workingDir(implicit key: Key) = s"VENDOR_${key.data}"
}

class VendorTaskLogConsumer @Inject()(
                                       vendorTaskDao: VendorTaskDao,
                                       vendorTaskResultDao: VendorTaskResultDao,
                                       fileOperator: FileOperator,
                                       config: Configuration,
                                       db: Database,
                                     )(implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {

  import profile.api._

  private def adapter(taskRow: VendorTaskRow) = AuditLogRow(taskRow.key, taskRow.status, -1, taskRow.uuid, taskRow.name, taskRow.createBy, Some(taskRow.location), createAt = Some(taskRow.createAt))

  override protected def auditLogRowGenerator(offset: Long, batchSize: Int): Future[Seq[AuditLogRow]] = {
    val sql = VendorTask.filter(_.key > offset).filter(_.isdeleted === false)
      .filter(x => x.status === VendorTaskDao.Status.VENDOR_PREPROCESSED || x.status === VendorTaskDao.Status.VENDOR_PROCESSING)
      .take(batchSize).result
    Future.successful(Await.result(db.run(sql), 2.seconds).map(adapter))
      .recover[Seq[AuditLogRow]] {
        case _: Throwable => Seq.empty
      }
  }

  override def logFilter: AuditLogRow => Boolean = row => row.event == VendorTaskDao.Status.VENDOR_PREPROCESSED || row.event == VendorTaskDao.Status.VENDOR_PROCESSING

  override def businessName: String = "log_committer_vendor_task"

  //  private var processingCount = 0
  //  private val maxProcessingCount = 1

  override def processor: Processor = (_: Option[AuditLogRow], cur: AuditLogRow) => {
    implicit val key: Key = Key(cur.id)
    val defaultNumberValueTitleRegex = ".*金额.*|.*余额.*|.*发生额.*"

    implicit def numberValueTitleRegex: NumberValueTitleRegex =
      NumberValueTitleRegex(config.getOptional[String]("idr.numberValueTitleRegex").getOrElse(defaultNumberValueTitleRegex));
    val status = cur.event
    val id = cur.entityId // uuid
    val name = cur.entityType
    val location = cur.message.getOrElse("error dsri")
    var paodingResponse = ""
    Try {
      status match {
        case VendorTaskDao.Status.VENDOR_PREPROCESSED =>
          Await.result(vendorTaskDao.updateStatus(id, VendorTaskDao.Status.VENDOR_PROCESSING), 2.seconds)
        case _ =>
      }
      val fileValue = FileValue(location, name)
      val documentRecord = Await.result(vendorTaskResultDao.get(id, VendorTaskResultDaoCreateIn.Type.DOCUMENT), 2.seconds).getOrElse(throw new RuntimeException("should have the document result but not"))
      val contentLines = VendorBusinessUtils.documentToLines(FileValue(documentRecord.location, documentRecord.name))
      contentLines.map(_.text_content).take(5).foreach(println)
      val keys = Seq(ExtractKey("客户名称", "户名|集团名称|客户名称|单位名称|姓名|账户名称|A/C name|本方户名"),
        ExtractKey("账户名称", "户名|集团名称|客户名称|单位名称|姓名|账户名称|A/C name|本方户名"),
        ExtractKey("银行账号", "银行账号|账号|账户号|户口号|A/C No"),
        ExtractKey("币种", "币种|币别|货币"),
        ExtractKey("开户行", "银行名称|开户银行|开户行|支行名称|开户机构|客户行|行名"))
      val info = VendorBusinessUtils.extract(contentLines, keys)
      info.foreach(println)
      val response: Response[String] = Await.result(Future.successful(submit(fileValue)), 10.hours)
      paodingResponse = response.result
      val paodingTable = VendorBusinessUtils.deserializerPaodingTable(paodingResponse)
      val (wbByteArray, bsOutputJsonString) = VendorBusinessUtils.prependInfo(paodingTable, info)
      val resultFileValue = FileEngine.createNewFile(VendorCommon.workingDir, "result", id)
      fileOperator.save(wbByteArray, resultFileValue)
      Await.result(vendorTaskResultDao.create(VendorTaskResultDaoCreateIn(id, s"${name.split("\\.").headOption.getOrElse(s"$name-e")}-result.xlsx", resultFileValue.dsri, VendorTaskResultDaoCreateIn.Type.FINAL)), 2.seconds)
      val jsonFileValue = FileEngine.createNewFile(VendorCommon.workingDir, "result", id+"_json")
      fileOperator.save(bsOutputJsonString.getBytes(), jsonFileValue)
      Await.result(vendorTaskResultDao.create(VendorTaskResultDaoCreateIn(id, s"${name.split("\\.").headOption.getOrElse(s"$name-e")}-result.json", jsonFileValue.dsri, VendorTaskResultDaoCreateIn.Type.JSON)), 2.seconds)
      Await.result(vendorTaskDao.updateStatus(id, VendorTaskDao.Status.VENDOR_PROCESSED), 2.seconds)
      Await.result(vendorTaskDao.updateExpireAt(id, new Timestamp(org.joda.time.DateTime.now().plusDays(1).toDate.getTime)), 2.seconds)
    } fold(ex => {
      LoggerFactory.getLogger(this.getClass).error("vendor process failed: " + ex.getMessage + " --- " + ex.getStackTrace.mkString("---") + "paodingResponse:" + paodingResponse)
      try Await.result(vendorTaskDao.updateStatus(id, VendorTaskDao.Status.VENDOR_PROCESS_FAILED), 2.seconds)
      catch {
        case _: Throwable => LoggerFactory.getLogger(this.getClass).error("database timeout Exception" + ex.getMessage + " --- " + ex.getStackTrace.mkString("---"))
      }
    }, _ => ())
    Future.successful(true)
  }

  final protected def submit(fileValue: FileValue): Response[String] = {
    val uuid = VendorPaodingClient.upload_file(fileValue.contentFile)
    VendorPaodingClient.wait_file_parsed(uuid, sys.env.getOrElse("VENDOR_TASK_MAX_RETRY", "60").toInt)
    //    val res: Array[Byte] = VendorPaodingClient.get_document_excel(uuid)
    val json: String = VendorPaodingClient.get_pdf_tables(uuid)

    // get_pdf_tables(uuid)
    // get_document_html(uuid)

    Response(json)
  }

  final protected def submit2(fileValue: FileValue): Response[Array[Byte]] = {
    val uuid = VendorPaodingClient.upload_file(fileValue.contentFile)
    VendorPaodingClient.wait_file_parsed(uuid, sys.env.getOrElse("VENDOR_TASK_MAX_RETRY", "60").toInt)
    val res = VendorPaodingClient.get_document_excel(uuid)
    Response(res)
  }

  case class Response[T](result: T)
}

object VendorPaodingClient {
  private val app_id: String = sys.env.getOrElse("PAI_APP_ID", "pdflux")
  private val secret_key: String = sys.env.getOrElse("PAI_SECRET_KEY", "aiYuVeeM4Ai4cheu")
  private val rootUrls: Seq[String] = sys.env.getOrElse("PAI_ROOT_URL", "http://10.158.15.46:31507/api/v1/saas").split(",").toSeq
  private val root_url: String = Random.shuffle(rootUrls).headOption.getOrElse(throw new RuntimeException("no root url for `PAI_ROOT_URL`"))
  private val user: String = sys.env.getOrElse("PAI_USER", "pwcds")


  import sttp.client4.quick
  import sttp.client4.quick._

  private def timeNow: Long = System.currentTimeMillis() / 1000

  private def generateToken(url: String, timeSeconds: Long): String = {
    val source = s"$url#$app_id#$secret_key#$timeSeconds"
    println("source -> " + source)
    val res = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8))
      .map("%02x".format(_)).mkString
    println("after digest -> " + res)
    res
  }

  def upload_file(file: java.io.File): String = {
    val timestamp = timeNow
    val url = uri"$root_url/upload?file_type=BS&force_update=true&user=$user"
    val token = generateToken(url.toString, timestamp)
    val finalUrl = uri"$url&_token=$token&_timestamp=$timestamp"
    println("finalUrl: " + finalUrl)
    println(file)
    val response = quickRequest
      .multipartBody(multipartFile("file", file))
      .post(finalUrl)
      .send()

    println("get response " + response)
    if (!response.isSuccess) throw new RuntimeException(s"upload_file response code ${response.code}, which is error code")

    val uuid: String = ujson.read(response.body)("data")("uuid").str

    println("get response uuid" + uuid)
    uuid
  }


  trait FileStatus

  object Error extends FileStatus

  object Pending extends FileStatus

  object Parsing extends FileStatus

  object Done extends FileStatus

  def get_file_status(uuid: String): FileStatus = {
    val timestamp = timeNow
    val url = uri"$root_url/document/$uuid?user=$user"
    val token = generateToken(url.toString, timestamp)
    val finalUrl = uri"$url&_token=$token&_timestamp=$timestamp"
    val response = quickRequest.get(finalUrl)
      .send()
    println("get_file_status response " + response)
    if (!response.isSuccess) throw new RuntimeException(s"get_file_status response code ${response.code}, which is error code")
    val result: Int = ujson.read(response.body)("data")("parsed").num.toInt
    println("get_file_status result: " + result)
    result match {
      case -1 => Error
      case 0 => Pending
      case 1 => Parsing
      case 2 => Done
      case _ => Error
    }
  }

  @tailrec
  def wait_file_parsed(uuid: String, maxRetry: Int): Unit = {
    if (maxRetry == 0) throw new RuntimeException("exceed max retry times!")
    println(s"polling $uuid ...... remains: $maxRetry")
    get_file_status(uuid) match {
      case Error => throw new RuntimeException("errors from vendor")
      case Pending | Parsing => Thread.sleep(1000 * 10); wait_file_parsed(uuid, maxRetry - 1)
      case Done => ()
    }
  }

  def get_pdf_tables(uuid: String): String = {
    val timestamp = timeNow
    val url = uri"$root_url/document/$uuid/pdftables?user=$user"
    val token = generateToken(url.toString, timestamp)
    val finalUrl = uri"$url&_token=$token&_timestamp=$timestamp"
    val response = quickRequest.get(finalUrl)
      .send()
    println("get_pdf_tables response " + "response")
    if (!response.isSuccess) throw new RuntimeException(s"get_pdf_tables response code ${response.code}, which is error code")
    println("get_pdf_tables response body " + "response.body")
    response.body
  }

  def get_document_html(uuid: String): String = {
    val timestamp = timeNow
    val url = uri"$root_url/document/$uuid/html?user=$user"
    val token = generateToken(url.toString, timestamp)
    val finalUrl = uri"$url&_token=$token&_timestamp=$timestamp"
    val response = quickRequest.get(finalUrl)
      .send()
    println("get_document_html response " + response)
    if (!response.isSuccess) throw new RuntimeException(s"get_document_html response code ${response.code}, which is error code")
    println("get_document_html response body " + response.body)
    response.body
  }

  def get_document_excel(uuid: String): Array[Byte] = {
    val timestamp = timeNow
    val url = uri"$root_url/document/$uuid/excel?user=$user"
    val token = generateToken(url.toString, timestamp)
    val finalUrl = uri"$url&_token=$token&_timestamp=$timestamp"
    val response = quickRequest.response(quick.asByteArrayAlways)
      .get(finalUrl)
      .send()
    println("get_document_excel response " + response)
    if (!response.isSuccess) throw new RuntimeException(s"get_document_excel response code ${response.code}, which is error code")
    println("get_document_excel response body " + response.body)
    response.body
  }

}

object VendorBusinessUtils {
  case class ExtractKey(name: String, pattern: String)

  case class Sentence(page_id: Option[Int], text_content: String)

  case class MetaPage(begin: Option[Int], end: Option[Int])

  case class ExtractedResult(page_id: Option[Int], result: String)

  object ExtractedResult {
    implicit val format: Format[ExtractedResult] = Json.format[ExtractedResult]
  }

  def deserializerPaodingTable(json: String): PaodingTable = Json.fromJson[PaodingTable](Json.parse(json))
    .fold(invalid => throw new RuntimeException(invalid.mkString("---")), identity)

  def documentToLines(fileValue: FileValue): Seq[Sentence] = {
    implicit val formats: Formats = Serialization.formats(NoTypeHints)
    var source: BufferedSource = null
    try {
      source = scala.io.Source.fromInputStream(fileValue.fileInputStream)
      val documentInfo: DocumentInfo = read[DocumentInfo](source.mkString)
      documentInfo.document.doc_units.flatMap(docUnit => docUnit.document_content.sentences).map(x => Sentence(x.page_id, x.text_content))
    } catch {
      case ex: Exception => {
        LoggerFactory.getLogger(this.getClass).warn("vendor process documentToLines failed: " + ex.getMessage + " --- " + ex.getStackTrace.mkString("---"))
        val res:Seq[Sentence] = Seq()
        res
      }
    } finally {
      if (source != null) source.close()
    }
  }

  def extract(text: Seq[Sentence], keys: Seq[ExtractKey]): Map[String, Seq[ExtractedResult]] = {
    var res: Map[String, Seq[ExtractedResult]] = keys.map {
      case ExtractKey(name, _) =>
        (name, Seq.empty[ExtractedResult])
    }.toMap
    val sentenceIterator = text.reverseIterator
    var endPageId: Option[Int] = None
    var prev: Option[Sentence] = None
    while (sentenceIterator.hasNext) {
      val sentence = sentenceIterator.next()
      if (prev.flatMap(_.page_id).nonEmpty && sentence.page_id.nonEmpty && prev.flatMap(_.page_id).get != sentence.page_id.get) {
        endPageId = prev.flatMap(_.page_id)
      }
      prev = Some(sentence)
      val pageId = sentence.page_id
      val trimedSentence = sentence.text_content.replace(" ", "")
      //      println("trimedSentence: " + trimedSentence)
      val splitedSentence = trimedSentence.split("""[\t:：]""")
      //      println("splitedSentence: " + splitedSentence.mkString("---"))
      val iterator = splitedSentence.iterator
      while (iterator.hasNext) {
        val slice = iterator.next()
        val keySeq = keys.filter { case ExtractKey(_, pattern) => slice.matches(pattern) }
        if (keySeq.nonEmpty && iterator.hasNext) {
          val value = iterator.next()
          keySeq.foreach {
            case ExtractKey(name, _) =>
              res = res + (name -> (res(name) ++ Seq(ExtractedResult(pageId, value))))
          }
        }
      }
    }
    res
  }

  case class CellValue(value: String)

  object CellValue {
    implicit val format: Format[CellValue] = Json.format[CellValue]
  }

  case class TitleCell(key: TitleCellKey, value: Seq[TitleCellValue])

  object TitleCell {
    case class TitleCellKey(text: String)

    object TitleCellKey {
      implicit val format: Format[TitleCellKey] = Json.format[TitleCellKey]
    }

    case class TitleCellValue(text: String)

    object TitleCellValue {
      implicit val format: Format[TitleCellValue] = Json.format[TitleCellValue]
    }

    implicit val format: Format[TitleCell] = Json.format[TitleCell]
  }

  case class BSOutput(header: Option[Map[String, Seq[ExtractedResult]]], tables: Option[Seq[BStable]])

  object BSOutput {
    implicit val format: Format[BSOutput] = Json.format[BSOutput]
  }
  case class BStable(page_id: Option[Int], result: Option[Map[String, Seq[TitleCellValue]]])
  object BStable {
    implicit val format: Format[BStable] = Json.format[BStable]
  }
  case class Element(cells: Option[Map[String, CellValue]], title_cells: Option[Seq[TitleCell]])

  object Element {
    implicit val format: Format[Element] = Json.format[Element]
  }

  case class PdfElement(page: Int, elements: Seq[Element])

  object PdfElement {
    implicit val format: Format[PdfElement] = Json.format[PdfElement]
  }

  case class PaodingTable(pdf_elements: Seq[PdfElement])

  object PaodingTable {
    implicit val format: Format[PaodingTable] = Json.format[PaodingTable]
  }

  case class TablePageRange(begin: Option[Int], end: Option[Int])

  def getTablePageRange(paodingTable: PaodingTable): Seq[TablePageRange] = {
    val pdfElementIterator = paodingTable.pdf_elements.reverseIterator
    var res = Seq.empty[TablePageRange]
    var prev: Option[Int] = None
    while (pdfElementIterator.hasNext) {
      val curPdfElement = pdfElementIterator.next()
      val pageId = curPdfElement.page
      if (prev.isEmpty || (prev.nonEmpty && prev.get != pageId)) {
        res = res :+ TablePageRange(Some(pageId), prev)
      }
      prev = Some(pageId)
    }
    res
  }

  private def rebasePageId(pageId: Int) = pageId + 1

  case class NumberValueTitleRegex(regex: String)

  def prependInfo(paodingTable: PaodingTable, info: Map[String, Seq[ExtractedResult]])(implicit numberValueTitleRegex: NumberValueTitleRegex): (Array[Byte],String) = {
    import com.pwc.ds.cidr.engine.util.PoiUtils._

    LoggerFactory.getLogger(this.getClass).info("numberValueTitleRegex is " + numberValueTitleRegex)

    val tableRangeSeq: Seq[TablePageRange] = getTablePageRange(paodingTable)
    //    println("tableRangeSeq: " + tableRangeSeq)
    val workbook = WorkbookFactory.create(true)

    var bsOutputJsonString = ""
    var bsTables: Seq[BStable]=Seq()
    val pdfElementIterator = paodingTable.pdf_elements.iterator
    var rowBase = info.size + 1
    while (pdfElementIterator.hasNext) {
      val curPdfElement = pdfElementIterator.next()
      val elementIterator = curPdfElement.elements.iterator
      val pageId = curPdfElement.page
      val TablePageRange(pageBegin, pageEnd) = tableRangeSeq.find(_.begin.get == pageId).get
      while (elementIterator.hasNext) {
        val curElement = elementIterator.next()
        var curSheet = workbook.getSheet(s"page ${rebasePageId(pageId)}")
        if (workbook.getSheet(s"page ${rebasePageId(pageId)}") == null){
          curSheet = workbook.createSheet(s"page ${rebasePageId(pageId)}")
        }else{
          val lastRows = curSheet.getLastRowNum()
          rowBase = lastRows + 2
        }
        // first write paoding results
        var tableCols: Map[String, Seq[TitleCellValue]]= Map()
        def writePaodingResult = {
          if (curElement.cells.nonEmpty) {
            val allCells = curElement.cells.get.map { case (key, cellValue) =>
              val row_col = key.split("_").toSeq
              val row = row_col.head.toInt + rowBase
              val col = row_col(1).toInt
              (row, col, cellValue.value)
            }.toSeq
            allCells.groupBy(_._2).mapValues(_.sorted)
              .values.foreach(colSeq => {
                var lastRow = rowBase
                val colSeqIterator = colSeq.iterator
                if (colSeqIterator.hasNext) {
                  val (row, col, title) = colSeqIterator.next()
                  var currentTitleCellValueSeq: Seq[TitleCellValue]=Seq()
                  var toBeFilledRowLength = row - lastRow
                  while (toBeFilledRowLength > 0) {
                    currentTitleCellValueSeq :+= TitleCellValue("")
                    toBeFilledRowLength -= 1
                  }
                  currentTitleCellValueSeq :+= TitleCellValue(title)
                  lastRow = row
                  val titleMatched = title.split('\n').map(_.trim.filter(_ >= ' ')).mkString.matches(numberValueTitleRegex.regex)
                  LoggerFactory.getLogger(this.getClass).info("match title1: " + title + "; matched: " + titleMatched)
                  curSheet.getRowOrCreateNew(row).createCell(col).setCellValue(title)
                  while (colSeqIterator.hasNext) {
                    try {
                      val (row, col, value) = colSeqIterator.next()
                      var toBeFilledRowLength = row - lastRow
                      while (toBeFilledRowLength > 1) {
                        currentTitleCellValueSeq :+= TitleCellValue("")
                        toBeFilledRowLength -= 1
                      }
                      currentTitleCellValueSeq :+= TitleCellValue(value)
                      lastRow=row
                      val cell = curSheet.getRowOrCreateNew(row).createCell(col)
                      if (titleMatched) cell.writeValue(value) else cell.setCellValue(value)
                    }
                    catch {
                      case _: Throwable =>
                    }
                  }
                  tableCols += (col.toString-> currentTitleCellValueSeq)
                }
              })
            bsTables :+= BStable(Some(pageId),Some(tableCols))
          } else if (curElement.title_cells.nonEmpty) {
            val titleCellIterator = curElement.title_cells.get.iterator
            var col = 0
            while (titleCellIterator.hasNext) {
              var row = rowBase
              val TitleCell(key, value) = titleCellIterator.next()
              val currentTitleCellValueSeq = TitleCellValue(key.text) +: value
              tableCols += (col.toString-> currentTitleCellValueSeq)
              curSheet.getRowOrCreateNew(row).createCell(col).setCellValue(key.text)
              val titleMatched = key.text.split('\n').map(_.trim.filter(_ >= ' ')).mkString.matches(numberValueTitleRegex.regex)
              LoggerFactory.getLogger(this.getClass).info("match title: " + key + "; matched: " + titleMatched)
              row += 1
              val valueIterator = value.iterator
              while (valueIterator.hasNext) {
                val TitleCellValue(curValue) = valueIterator.next()
                val cell = curSheet.getRowOrCreateNew(row).createCell(col)
                if (titleMatched) cell.writeValue(curValue) else cell.setCellValue(curValue)
                row += 1
              }
              col += 1
            }
            bsTables :+= BStable(Some(pageId),Some(tableCols))
          } else {
            throw new RuntimeException(s"broken json from paoding: $paodingTable")
          }
        }

        writePaodingResult

        // second, write extracted table headers
        def writeExtractTableHeaders = {
          var row = 0
          val infoEntryIterator = info.iterator
          while (infoEntryIterator.hasNext) {
            val curRow = curSheet.getRowOrCreateNew(row)
            val (key, value) = infoEntryIterator.next()
            var curCell = curRow.createCell(0, CellType.STRING)
            curCell.setCellValue(key)
            curCell = curRow.createCell(1, CellType.STRING)
            curCell.setCellValue(value.filter(x => x.page_id.get >= pageBegin.get && (pageEnd.isEmpty || x.page_id.get < pageEnd.get)).map(_.result).reverse.headOption.getOrElse(""))
            row += 1
          }
        }

        writeExtractTableHeaders
      }
    }
    val bsOutput = BSOutput(Some(info), Some(bsTables))
    bsOutputJsonString = Json.toJson(bsOutput).toString()
    val result = new ByteArrayOutputStream()
    try {
      workbook.write(result)
    }
    finally {
      result.close()
    }
    (result.toByteArray,bsOutputJsonString)
  }

  // it doesn't work: shiftRows will throw exception
  //  def prependInfo(byteArray: Array[Byte], info: Map[String, Seq[ExtractedResult]]): Array[Byte] = {
  //    ???
  //    val inputStream = new ByteArrayInputStream(byteArray)
  //    val excel: Workbook = WorkbookFactory.create(inputStream)
  //    val curSheet = excel.getSheetAt(0)
  //    curSheet.shiftRows(2, curSheet.getLastRowNum, 1)
  //    //    val row = curSheet.createRow(1)
  //
  //    //    val sheetIterator = excel.sheetIterator()
  //    //    while (sheetIterator.hasNext) {
  //    //      val curSheet = sheetIterator.next()
  //    //      curSheet.shiftRows(curSheet.getFirstRowNum, curSheet.getLastRowNum, 1)
  //    ////      var rowIterator = curSheet.rowIterator()
  //    //      //      var pre: Row = null
  //    //      //      var cur: Row = null
  //    //      //      while (rowIterator.hasNext) {
  //    //      //        cur = rowIterator.next()
  //    //      //        curSheet.shiftRows()
  //    //      //      }
  //    //      //      rowIterator = curSheet.rowIterator()
  //    //
  //    //    val row = curSheet.createRow(0)
  //    ////      if (rowIterator.hasNext) {
  //    ////        val firstRow = rowIterator.next()
  //    ////        val infoEntryIterator = info.iterator
  //    ////        var col = 0
  //    ////        while (infoEntryIterator.hasNext) {
  //    ////          val (key, value) = infoEntryIterator.next()
  //    ////          var curCell = firstRow.createCell(col, CellType.STRING)
  //    ////          curCell.setCellValue(key)
  //    ////          col += 1
  //    ////          curCell = firstRow.createCell(col, CellType.STRING)
  //    ////          curCell.setCellValue(value.head)
  //    ////          col += 2
  //    ////        }
  //    ////      }
  //    //    }
  //    val result = new ByteArrayOutputStream()
  //    try {
  //      excel.write(result)
  //    }
  //    finally {
  //      result.close()
  //    }
  //    result.toByteArray
  //  }

}