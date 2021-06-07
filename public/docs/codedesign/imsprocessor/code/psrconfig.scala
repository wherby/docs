
object PurchaseTransactionReportConfig {
  val extractorConfig = ExtractorConfig(
    "PURCHASE TRANSACTION REPORT",
    Seq(
      Seq(
        ColumnConfig("", 0, 143),
        ColumnConfig("", 143, 158)
      ),
      Seq(
        ColumnConfig("", 0, 160),
        ColumnConfig("", 160, 168)
      ),
      Seq(
        ColumnConfig("", 0, 160)
      ),
      Seq(
        ColumnConfig("", 0, 53),
        ColumnConfig(ColumnTitles.START_DATE, 58, 66),
        ColumnConfig(ColumnTitles.END_DATE, 70, 78)
      ),
      Seq(
        ColumnConfig(ColumnTitles.FUND_NAME, 0, 160)
      ),
      Seq(
        ColumnConfig(ColumnTitles.FUND_NAME, 0, 160)
      )
    ),
    TableConfig(
      ".*T R A D E D  C U R R E N C Y  /  B A S E  C U R R E N C Y.*",
      4,
      tableBodyConfig = TableBodyConfig(
        rows = Seq(
          TableRowConfig(
            Seq(
              ColumnConfig("TD DATE/", 0, 9),
              ColumnConfig("BROKER", 9, 25),
              ColumnConfig("TRANS #", 25, 35),
              ColumnConfig("NUMBER OF SHARES", 35, 56),
              ColumnConfig("CCY_1", 56, 59),
              ColumnConfig("UNIT PRICE_1", 59, 76),
              ColumnConfig("PRINCIPAL COST_1", 76, 94),
              ColumnConfig("BROKER COMMISSION_1", 94, 113),
              ColumnConfig("OTHER FEES_1", 113, 131),
              ColumnConfig("SETTLEMENT AMOUNT_1", 131, 150),
              ColumnConfig("INT PURCHASE_1", 150, 165)
            )
          ),
          TableRowConfig(
            Seq(
              ColumnConfig("STL DATE", 0, 9),
              ColumnConfig("SECURITY NAME_1", 9, 56),
              ColumnConfig("CCY_2", 56, 59),
              ColumnConfig("UNIT PRICE_2", 59, 76),
              ColumnConfig("PRINCIPAL COST_2", 76, 94),
              ColumnConfig("BROKER COMMISSION_2", 94, 113),
              ColumnConfig("OTHER FEES_2", 113, 131),
              ColumnConfig("SETTLEMENT AMOUNT_2", 131, 150),
              ColumnConfig("INT PURCHASE_2", 150, 165)
            )
          ),
          TableRowConfig(
            Seq(
              ColumnConfig("SECURITY NAME_2", 0, 59),
              ColumnConfig("PRINCIPAL COST_3", 81, 84)
            ),
            true
          ),
          TableRowConfig(
            Seq(
              ColumnConfig("SECURITY ID & CODE", 0, 59)
            )
          )
        ),
        s"\\s*$dateRegex.*"
      ),
      summaries = Seq(
        // Ignore the TRADED CCY TOTAL row and BASE CCY TOTAL row
      ),
      separators = Seq(
        TableSeparatorConfig(
          "---------------",
          1,
          1,
          rows = Seq(
            Seq(
              ColumnConfig("", 0, 165)
            ),
            // skip the middle line of the separator
            Seq(),
            Seq(
              ColumnConfig("GenericInvestment", 0, 165)
            )
          ),
          appendToBody = true
        )
      ),
      rowCount = None,
      subTable = Some(
        TableConfig(
          ".*BASE CURRENCY.*",
          3,
          TableBodyConfig(
            rows = Seq(
              TableRowConfig(
                Seq(
                  ColumnConfig("TD DATE/", 0, 56, 4),
                  ColumnConfig("CCY_1", 56, 59),
                  ColumnConfig("UNIT PRICE_1", 59, 76),
                  ColumnConfig("PRINCIPAL COST_1", 76, 94),
                  ColumnConfig("BROKER COMMISSION_1", 94, 113),
                  ColumnConfig("OTHER FEES_1", 113, 131),
                  ColumnConfig("SETTLEMENT AMOUNT_1", 131, 150),
                  ColumnConfig("INT PURCHASE_1", 150, 165)
                )
              )
            ),
            indicator = ".*GRAND TOTAL.*"
          ),
          rowCount = Some(1)
        )
      ),
      reportHeaderLineNumber = 6
    ),
    Some(
      TableConfig(tableBodyConfig =
        TableBodyConfig(
          Seq(TableRowConfig(Seq(ColumnConfig("", 0, 75), ColumnConfig("", 76, 92)))),
          ""
        )
      )
    )
  )

  val singleCurrencyBodyRowConfig = TableBodyConfig(
    Seq(
      TableRowConfig(
        Seq(
          ColumnConfig("TD DATE/", 0, 9),
          ColumnConfig("BROKER", 9, 25),
          ColumnConfig("TRANS #", 25, 35),
          ColumnConfig("NUMBER OF SHARES", 35, 56),
          ColumnConfig("CCY_1", 56, 59),
          ColumnConfig("UNIT PRICE_1", 59, 76),
          ColumnConfig("PRINCIPAL COST_1", 76, 94),
          ColumnConfig("BROKER COMMISSION_1", 94, 113),
          ColumnConfig("OTHER FEES_1", 113, 131),
          ColumnConfig("SETTLEMENT AMOUNT_1", 131, 150),
          ColumnConfig("INT PURCHASE_1", 150, 165),
          //Copy the value as BASE CCY
          ColumnConfig("CCY_2", 56, 59),
          ColumnConfig("UNIT PRICE_2", 59, 76),
          ColumnConfig("PRINCIPAL COST_2", 76, 94),
          ColumnConfig("BROKER COMMISSION_2", 94, 113),
          ColumnConfig("OTHER FEES_2", 113, 131),
          ColumnConfig("SETTLEMENT AMOUNT_2", 131, 150),
          ColumnConfig("INT PURCHASE_2", 150, 165)
        )
      ),
      TableRowConfig(
        Seq(
          ColumnConfig("STL DATE", 0, 9),
          ColumnConfig("SECURITY NAME_1", 9, 56)
        )
      ),
      TableRowConfig(
        Seq(
          ColumnConfig("SECURITY NAME_2", 0, 59),
          ColumnConfig("PRINCIPAL COST_3", 81, 84)
        ),
        true
      ),
      TableRowConfig(
        Seq(
          ColumnConfig("SECURITY ID & CODE", 0, 59)
        )
      )
    ),
    s"\\s*$dateRegex.*"
  )

  val sanitizeConfig = SanitizerConfig(
    ignores = Seq(
      SanitizerLineConfig("PRICE IS OUTSIDE TOLERANCE", true),
      SanitizerLineConfig("SYSTEM DATE")
    )
  )

  val outputterConfig = ExcelOutputConfig(
    false,
    Seq(
      "TD DATE/",
      "STL DATE",
      "BROKER",
      "SECURITY NAME",
      "SECURITY ID & CODE",
      "TRANS #",
      "NUMBER OF SHARES",
      "CCY_1",
      "UNIT PRICE_1",
      "PRINCIPAL COST_1",
      "BROKER COMMISSION_1",
      "OTHER FEES_1",
      "SETTLEMENT AMOUNT_1",
      "INT PURCHASE_1",
      "CCY_2",
      "UNIT PRICE_2",
      "PRINCIPAL COST_2",
      "BROKER COMMISSION_2",
      "OTHER FEES_2",
      "SETTLEMENT AMOUNT_2",
      "INT PURCHASE_2",
      "PRINCIPAL COST_3"
    )
  )
}