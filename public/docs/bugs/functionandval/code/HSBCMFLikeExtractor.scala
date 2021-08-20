import io.github.jonathanlink.PDFLayoutTextStripper
import org.apache.pdfbox.pdmodel.PDDocument

import java.io.File

case class HSBCMFLikeExtractor(
                                removeHeader: Seq[String],
                                splitPosition: Seq[Int],
                                trimContentLine: Option[Int] = None,
                                tableToRecode: Option[(Seq[String], Int) => Seq[Seq[String]]] = None,
                                splitSeq: Seq[(Int, (String => Seq[String]))] = Seq()
                              )

object HSBCMFLikeExtractor {
  def getContentStr(filePath: String) = {
    val file        = new File(filePath);
    val document    = PDDocument.load(file);
    val pdfStripper = new PDFLayoutTextStripper()
    pdfStripper.getText(document)
  }

  def getTableContent(listStr: Seq[String], removeStr: Seq[String]) = {
    val items = splitByEmptyLine(listStr)
    items.filter(item => {
      removeStr.foldLeft(true)((b, str1) => b && item(0).indexOf(str1) < 0)
    })
  }

  private def splitByEmptyLine(listStr: Seq[String]): Seq[Seq[String]] = {
    var res     = Seq[Seq[String]]()
    var tempRes = Seq[String]()
    for (line <- listStr) {
      if (line.trim().length == 0) {
        if (tempRes.length > 0) {
          res = res :+ tempRes
          tempRes = Seq()
        }
      } else {
        tempRes = tempRes :+ line
      }
    }
    if (tempRes.length > 0) {
      res = res :+ tempRes
    }
    res
  }

  private def getModifiedSplitPoint(strLine: String, splitPoint: Seq[Int]) = {
    splitPoint.map { point =>
      var pointTemp = point - 1
      while (pointTemp < strLine.length - 1 && strLine(pointTemp) != ' ') {
        pointTemp = pointTemp + 1
      }
      pointTemp
    }
  }

  //suppose every record has the one none-empty beginning value for the record number at line 1.
  val splitTableToRecords = (tableStr: Seq[String], sectionIndex: Int) => {
    var res: Seq[Seq[String]] = Seq()
    var tempRes: Seq[String]  = Seq()
    for (line <- tableStr) {
      if (line.substring(0, sectionIndex).trim.length != 0 && tempRes.length > 0) {
        res = res :+ tempRes
        tempRes = Seq(line)
      } else {
        tempRes = tempRes :+ line
      }
    }
    if (tempRes.length > 0) {
      res = res :+ tempRes
    }
    res
  }

  def splitLine(
                 strLine: String,
                 splitPoint: Seq[Int],
                 splitFunSeq: Seq[(Int, (String => Seq[String]))]
               ) = {
    var res: Seq[String] = Seq()
    var start            = 0
    for (point <- splitPoint) {
      res = res :+ strLine.substring(start, point)
      start = point
    }
    res = res :+ strLine.substring(start)
    val functionMap = splitFunSeq.toMap
    res.zipWithIndex.flatMap {
      case (line, index) => {
        functionMap.get(index) match {
          case Some(fn) =>
            fn(line)
          case _ => Seq(line)
        }
      }
    }
  }

  def splitSectionAndJoin(
                           itemLines: Seq[String],
                           splitPoint: Seq[Int],
                           splitFunSeq: Seq[(Int, (String => Seq[String]))] = Seq()
                         ) = {
    val res = itemLines
      .map { itemline =>
        val modifiedPoints = getModifiedSplitPoint(itemline, splitPoint)
        splitLine(itemline, modifiedPoints, splitFunSeq)
      }
    res.tail.foldLeft(res.head)((r, itemLine) => jointSeqStr(r, itemLine))
  }

  private def jointSeqStr(seqS1: Seq[String], seqS2: Seq[String]) = {
    seqS1.zip(seqS2).map { case (a, b) => (a.trim + " " + b.trim).trim }
  }

  def extractRecord(hsbcMFLikeExtractor: HSBCMFLikeExtractor, content: String): Seq[Seq[String]] = {
    val isContentSuit =
      hsbcMFLikeExtractor.removeHeader.foldLeft(true)((b, str1) => content.indexOf(str1) >= 0 && b)
    val recordSpliterFn = hsbcMFLikeExtractor.tableToRecode.getOrElse(splitTableToRecords)
    isContentSuit match {
      case true =>
        val lines         = SinglePageExtractor.splitToLines(content)
        val tableContents = getTableContent(lines, hsbcMFLikeExtractor.removeHeader)
        val afterTrimTableHeader = hsbcMFLikeExtractor.trimContentLine match {
          case Some(lineNumber) =>
            tableContents.map { tableContent =>
              tableContent.slice(lineNumber, tableContent.length)
            }
          case _ => tableContents
        }
        afterTrimTableHeader
          .flatMap { tableContent =>
            recordSpliterFn(tableContent, hsbcMFLikeExtractor.splitPosition(0))
          }
          .map { record =>
            splitSectionAndJoin(
              record,
              hsbcMFLikeExtractor.splitPosition,
              hsbcMFLikeExtractor.splitSeq
            )
          }
          .map { record =>
            record.map { x => x.trim }
          }
      case _ => Seq()
    }
  }

  def extractRecordsFromFile(
                              hsbcMFLikeExtractor: HSBCMFLikeExtractor,
                              fileName: String
                            ): Seq[Seq[String]] = {
    val content = getContentStr(fileName)
    extractRecord(hsbcMFLikeExtractor, content)
  }
}
