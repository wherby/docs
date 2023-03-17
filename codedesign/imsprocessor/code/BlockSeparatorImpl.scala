trait BlockSeparatorImpl extends BlockSeparator {

  val endOfReport = "END OF REPORT";

  var dataSanitizer: BaseDataSanitizerImpl = new BaseDataSanitizerImpl {}

  var isReportHeaderPaternStrictMode: Option[Boolean] = None

  /** * @throws java.lang.NumberFormatException  If the string does not contain a parsable `Double`.
   * * @throws java.lang.NullPointerException  If the string is null.
   *
   * @param str
   * @return
   */
  override def parseDouble(str: String): Double = {
    try {
      str.replaceAll(",", "").toDouble
    } catch {
      case x: NumberFormatException => {
        SafeLogger.logStringError(Logger, str + " can't be parse into double. ", x)
        0
      }
      case e: Exception => {
        SafeLogger.logStringError(Logger, str + " can't be parse into double. ", e)
        0
      }
    }
  }

  override def splitContentIntoPages(
                                      content: String,
                                      lineBreaker: String,
                                      pageLineNum: Int = 1
                                    ): Array[String] = {
    var splitPagePattern = ""
    if (pageLineNum == 1) {
      splitPagePattern = s"(?=[ \\t]*${lineBreaker}.*PAGE(\\s)*(\\d)+)"
    } else if (pageLineNum == 2) {
      splitPagePattern = s"(?=[ \\t]*${lineBreaker}.*${lineBreaker}.*PAGE(\\s)*(\\d)+)"
    } else {
      splitPagePattern = "(?=(.*" + lineBreaker + "){" + pageLineNum + "}.**PAGE(\\s)*(\\d)+)";
    }
    var pages = content.split(splitPagePattern);
    pages = pages.filter(page => {
      !dataSanitizer.isGarbageContent(page) && page.contains("PAGE")
    })
    pages
  }

  override def splitContentIntoSegments(
                                         content: String,
                                         lineBreaker: String,
                                         indicator: String
                                       ): Array[String] = {
    var splitPagePattern = "(?=" + lineBreaker + ".*" + indicator + ")";
    var segments         = content.split(splitPagePattern);
    segments = segments.filter(!dataSanitizer.isGarbageContent(_)) // to filter empty block
    segments
  }

  /** there maybe two kinds of report header patterns, one contains space line in the report header, one does not. this function find out which, and returns.
   * @param pageContent
   * @param reportHeaderLineNumber
   * @param lineBreaker
   * @return
   */
  def getReportHeaderPattern(
                              pageContent: String,
                              reportHeaderLineNumber: Int,
                              lineBreaker: String
                            ): String = {
    isReportHeaderPaternStrictMode match {
      case Some(true) => "^(.*\\w+.*" + lineBreaker + "){" + reportHeaderLineNumber + "}"
      case Some(false) =>
        "^(.*" + lineBreaker + "){" + reportHeaderLineNumber + ",}?(\\s*" + lineBreaker + ")"
      case _ => {
        var headerPattern1 = "^(.*\\w+.*" + lineBreaker + "){" + reportHeaderLineNumber + "}";
        var headerPattern2 =
          "^(.*" + lineBreaker + "){" + reportHeaderLineNumber + ",}?(\\s*" + lineBreaker + ")";
        if (headerPattern1.r.findFirstMatchIn(pageContent).isDefined) {
          isReportHeaderPaternStrictMode = Some(true)
          headerPattern1
        } else {
          isReportHeaderPaternStrictMode = Some(false)
          headerPattern2
        }
      }
    }
  }

  /** get report header end index in the page content, the page content should start with the report header.
   * @param pageContent
   * @param reportHeaderLineNumber
   * @return
   */
  override def getReportHeaderEndIndex(
                                        pageContent: String,
                                        reportHeaderLineNumber: Int,
                                        lineBreaker: String
                                      ): Int = {
    var left          = dataSanitizer.trimEmptyStartLines(pageContent, lineBreaker)
    var headerPattern = getReportHeaderPattern(pageContent, reportHeaderLineNumber, lineBreaker)
    var headerReg     = headerPattern.r.findFirstMatchIn(left);
    headerReg match {
      case Some(headerMatch) => {
        headerMatch.end
      }
      case _ => {
        //TODO the page content is wrong, need error handling
        throw new Exception("there is no report header in this page")
      }
    }
  }

  /** get the table header end index in the page content. the page content should start with the table header.
   * @param pageContent
   * @param tableHeaderLineNumber
   * @return
   */
  override def getTableHeaderEndIndex(
                                       pageContent: String,
                                       tableHeaderLineNumber: Int,
                                       lineBreaker: String
                                     ): Int = {
    var headerAfterFirstRow = if (tableHeaderLineNumber > 1) {
      "(.*" + lineBreaker + "){" + (tableHeaderLineNumber - 1).toString + "}"
    } else { "" }
    var txtTableHeaderPattern = "^(.*\\w+.*" + lineBreaker + headerAfterFirstRow + ")"
    //var txtTableHeaderPattern = "^(.*\\w+.*" + lineBreaker + "){" + tableHeaderLineNumber + "}";
    var txtTableHeaderReg = txtTableHeaderPattern.r.findFirstMatchIn(pageContent);
    txtTableHeaderReg match {
      case Some(titleMatch) => {
        titleMatch.end
      }
      case _ => {
        -1
      }
    }
  }

  /** @param sheetContent
   * @param tableConfig
   * @param lineBreaker
   * @param hasSummary if this report contains the summary part, the summary part must be surrounded with stars ******
   * @return
   */
  override def getPageBlocks(
                              sheetContent: String,
                              tableConfig: TableConfig,
                              lineBreaker: String,
                              hasSummary: Boolean = false,
                              pageLineNumber: Int = 1
                            ): (Seq[BlockConfig], Seq[BlockConfig], Seq[BlockConfig]) = {
    var sheets: Array[String] = splitContentIntoSheets(sheetContent);
    var pages: Array[String] = splitContentIntoPages(
      sheetContent,
      lineBreaker,
      pageLineNumber
    ) // the pages must contain word "PAGE"

    var reportHeaders = Seq[BlockConfig]()
    var tablebodies   = Seq[BlockConfig]()
    var summaries     = Seq[BlockConfig]()

    for (pageNum <- 0 until pages.length) {
      var reportHeaderStr = ""
      var tableContent    = ""
      if (!dataSanitizer.isGarbageContent(pages(pageNum))) { // when page is summary page, it's empty content
        var page = pages(pageNum)
        var left = dataSanitizer.trimEmptyStartLines(page, lineBreaker)
        var reportHeaderLastIndex =
          getReportHeaderEndIndex(left, tableConfig.reportHeaderLineNumber, lineBreaker);
        reportHeaderStr = left.substring(0, reportHeaderLastIndex);
        left = left.substring(reportHeaderLastIndex)

        var (summaryContent: String, tableBodyContent: String) =
          if (hasSummary) getSummaryContent(left, lineBreaker)
          else ("", left) // here get summary content by star (*) lines
        summaries = summaries :+ BlockConfig(summaryContent, pageNum)
        tableContent = dataSanitizer.trimEmptyStartLines(tableBodyContent, lineBreaker)
      }
      reportHeaders = reportHeaders :+ BlockConfig(reportHeaderStr, pageNum)
      tablebodies = tablebodies :+ BlockConfig(tableContent, pageNum)
    }
    (reportHeaders, tablebodies, summaries)
  }

  override def splitContentIntoSheets(
                                       content: String,
                                       sheetSeparator: String = null
                                     ): Array[String] = {

    var splitSheetPattern = "(?<=" + endOfReport + ")";
    if (sheetSeparator != null) {
      splitSheetPattern = "(?<=" + sheetSeparator + ")"
    }

    var sheets = content.split(splitSheetPattern);
    sheets = sheets.filter(!dataSanitizer.isGarbageContent(_))

    sheets = sheets.filter(sheet => {
      sheet.contains("PAGE")
    })
    sheets
  }

  override def getTotalPageNumber(content: String): Int = {
    var pagePattern = "PAGE\\s*([1-9]\\d*)\\b".r;
    var matches     = pagePattern.findAllMatchIn(content);

    var pageNum: Int = 1;
    matches.foreach(regMatch => {
      var currentPageNum = Integer.parseInt(regMatch.group(1));
      if (currentPageNum > pageNum) {
        pageNum = currentPageNum
      }
    })
    pageNum
  }

  /** @param blockSeperatorLineNumber
   * @param tableHeaderLineNum: it's to instruct how many lines one block contains
   * @param tableBodyContent : this content only contains table body, without summary or others.
   * @return
   */
  override def splitTableContentIntoBlocks(
                                            blockSeperatorLineNumber: Int,
                                            tableHeaderLineNum: Int,
                                            tableContent: String,
                                            lineBreaker: String
                                          ): Seq[BlockConfig] = {

    var tableBodyContent = dataSanitizer.trimEmptyStartLines(tableContent, lineBreaker)
    var pageBlocks       = Seq[BlockConfig]();
    if (blockSeperatorLineNumber == 1) {

      if (tableHeaderLineNum == 1) {
        pageBlocks =
          tableBodyContent.split(lineBreaker).toSeq.map(blockContent => BlockConfig(blockContent))
      } else {
        var bodylines = tableBodyContent.split(lineBreaker);

        for (bodylineNum <- 0 until bodylines.length by tableHeaderLineNum) {
          var blockContent = "";
          for (blockLineNum <- 0 until tableHeaderLineNum) {
            if (bodylines.length > (bodylineNum + blockLineNum)) { // ensure it's not out of range when accessing bodylines
              blockContent = blockContent + lineBreaker + bodylines(bodylineNum + blockLineNum)
            }
          }
          pageBlocks = pageBlocks :+ BlockConfig(blockContent);
        }
      }

    } else if (blockSeperatorLineNumber >= 2) {
      pageBlocks = tableBodyContent
        .split("(\\s*" + lineBreaker + "){" + blockSeperatorLineNumber + "}")
        .toSeq
        .map(blockContent => BlockConfig(blockContent));
    }

    pageBlocks
  }

  /** summary content is surrounded with star lines both at the beginning and at the ending
   * @param content
   * @return
   */
  override def getSummaryContent(content: String, lineBreaker: String): (String, String) = {
    var left           = content;
    var summaryContent = "";
    var summaryPattern = ("\\*{5,}(.*" + lineBreaker + ")+\\*{5,}").r;
    summaryPattern.findFirstMatchIn(content) match {
      case Some(value) => {
        summaryContent = value.toString;
        summaryContent = dataSanitizer.trimEmptyStartLines(summaryContent, lineBreaker);
        summaryContent = dataSanitizer.trimEmptyEndLines(summaryContent, lineBreaker);
        left = content.substring(0, value.start)
        left = dataSanitizer.trimEmptyStartLines(left, lineBreaker);
        left = dataSanitizer.trimEmptyEndLines(left, lineBreaker)
      }
      case _ => {}
    }
    (summaryContent, left)
  }

}