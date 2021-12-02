
object SalesTransactionReportConfig {
  val extractorConfig = ExtractorConfig(
    "SALES TRANSACTION REPORT",
    Seq(
      Seq(
        ColumnConfig("", 0, 173),
        ColumnConfig("", 173, 188)
      ),
      Seq(
        ColumnConfig("", 0, 190),
        ColumnConfig("", 190, 198)
      ),
      Seq(
        ColumnConfig("", 0, 198)
      ),
      Seq(
        ColumnConfig("Group1", 0, 5),
        ColumnConfig("", 5, 73),
        ColumnConfig(ColumnTitles.START_DATE, 78, 86),
        ColumnConfig(ColumnTitles.END_DATE, 90, 98)
      ),
      Seq(
        ColumnConfig(ColumnTitles.FUND_NAME, 0, 198)
      ),
      Seq(
        ColumnConfig(ColumnTitles.FUND_NAME, 0, 198)
      )
    ),
    TableConfig(
      ".*T R A D E D  C U R R E N C Y  /  B A S E  C U R R E N C Y.*",
      4,
      TableBodyConfig(
        Seq(
          TableRowConfig(
            Seq(
              ColumnConfig("NUMBER OF SHARES", 18, 198)
            )
          ),
          /*
        There are totally 4 REALISED PROFIT/LOSS(-) in one table row. The first two values maps to CCY_1, so named REALISED PROFIT/LOSS(-)_1_1 and REALISED PROFIT/LOSS(-)_1_2
        The second two values maps to CCY_2 so named REALISED PROFIT/LOSS(-)_2_1 and REALISED PROFIT/LOSS(-)_2_2
           */
          TableRowConfig(
            Seq(
              ColumnConfig("TD DATE/", 0, 8),
              ColumnConfig("BROKER", 8, 25),
              ColumnConfig("TRANS #", 25, 44),
              ColumnConfig("CCY_1", 44, 47),
              ColumnConfig("UNIT PRICE_1", 47, 61),
              ColumnConfig("NET PROCEEDS_1", 61, 84),
              ColumnConfig("COST WRITTEN-OFF_1", 84, 106),
              ColumnConfig("REALISED PROFIT/LOSS(-)_1_1", 106, 125),
              ColumnConfig("BROKER COMMISSION_1", 125, 143),
              ColumnConfig("OTHER FEES_1", 143, 164),
              ColumnConfig("INT SOLD_1", 164, 179),
              ColumnConfig("DEFERRED INCOME_1", 179, 198)
            )
          ),
          TableRowConfig(
            Seq(
              ColumnConfig("PRINCIPAL COST_1", 61, 84),
              ColumnConfig("REALISED PROFIT/LOSS(-)_1_2", 106, 125)
            )
          ),
          TableRowConfig(
            Seq(
              ColumnConfig("STL DATE", 0, 8),
              ColumnConfig("SECURITY NAME_1", 8, 44),
              ColumnConfig("CCY_2", 44, 47),
              ColumnConfig("UNIT PRICE_2", 47, 61),
              ColumnConfig("NET PROCEEDS_2", 61, 84),
              ColumnConfig("COST WRITTEN-OFF_2", 84, 106),
              ColumnConfig("REALISED PROFIT/LOSS(-)_2_1", 106, 125),
              ColumnConfig("BROKER COMMISSION_2", 125, 143),
              ColumnConfig("OTHER FEES_2", 143, 164),
              ColumnConfig("INT SOLD_2", 164, 179),
              ColumnConfig("DEFERRED INCOME_2", 179, 198)
            )
          ),
          TableRowConfig(
            Seq(
              ColumnConfig("SECURITY NAME_2", 8, 44),
              ColumnConfig("PRINCIPAL COST_2", 61, 84),
              ColumnConfig("REALISED PROFIT/LOSS(-)_2_2", 106, 125)
            )
          ),
          TableRowConfig(
            Seq(
              ColumnConfig("SECURITY NAME_3", 8, 44)
            ),
            true
          ),
          TableRowConfig(
            Seq(
              ColumnConfig("SECURITY ID & CODE", 0, 106)
            )
          )
        ),
        "NUMBER OF SHARES :.*"
      ),
      Seq(
        //Ignore *total* rows
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
          4,
          TableBodyConfig(
            rows = Seq(
              TableRowConfig(
                Seq(
                  ColumnConfig("TD DATE/", 0, 44, 4),
                  ColumnConfig("CCY_1", 44, 47),
                  ColumnConfig("UNIT PRICE_1", 47, 61),
                  ColumnConfig("NET PROCEEDS_1", 61, 84),
                  ColumnConfig("COST WRITTEN-OFF_1", 84, 106),
                  ColumnConfig("REALISED PROFIT/LOSS(-)_1_1", 106, 125),
                  ColumnConfig("BROKER COMMISSION_1", 125, 143),
                  ColumnConfig("OTHER FEES_1", 143, 164),
                  ColumnConfig("INT SOLD_1", 164, 179),
                  ColumnConfig("DEFERRED INCOME_1", 179, 198)
                )
              ),
              TableRowConfig(
                Seq(
                  ColumnConfig("PRINCIPAL COST_1", 61, 84),
                  ColumnConfig("REALISED PROFIT/LOSS(-)_1_2", 106, 125)
                )
              )
            ),
            //Ignore the COUNTRY TOTAL row, only keep the GRAND TOTAL row
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
          Seq(
            TableRowConfig(
              Seq(
                ColumnConfig("", 0, 75),
                ColumnConfig("", 76, 92)
              )
            )
          ),
          ""
        )
      )
    )
  )

  val singleCurrencyBodyRowConfig = TableBodyConfig(
    Seq(
      TableRowConfig(
        Seq(
          ColumnConfig("NUMBER OF SHARES", 18, 198)
        )
      ),
      /*
      There are totally 4 REALISED PROFIT/LOSS(-) in one table row. The first two values maps to CCY_1, so named REALISED PROFIT/LOSS(-)_1_1 and REALISED PROFIT/LOSS(-)_1_2
      The second two values maps to CCY_2 so named REALISED PROFIT/LOSS(-)_2_1 and REALISED PROFIT/LOSS(-)_2_2
       */
      TableRowConfig(
        Seq(
          ColumnConfig("TD DATE/", 0, 8),
          ColumnConfig("BROKER", 8, 25),
          ColumnConfig("TRANS #", 25, 44),
          ColumnConfig("CCY_1", 44, 47),
          ColumnConfig("UNIT PRICE_1", 47, 61),
          ColumnConfig("NET PROCEEDS_1", 61, 84),
          ColumnConfig("COST WRITTEN-OFF_1", 84, 106),
          ColumnConfig("REALISED PROFIT/LOSS(-)_1_1", 106, 125),
          ColumnConfig("BROKER COMMISSION_1", 125, 143),
          ColumnConfig("OTHER FEES_1", 143, 164),
          ColumnConfig("INT SOLD_1", 164, 179),
          ColumnConfig("DEFERRED INCOME_1", 179, 198),
          //Copy the values as BASE CCY
          ColumnConfig("CCY_2", 44, 47),
          ColumnConfig("UNIT PRICE_2", 47, 61),
          ColumnConfig("NET PROCEEDS_2", 61, 84),
          ColumnConfig("COST WRITTEN-OFF_2", 84, 106),
          ColumnConfig("REALISED PROFIT/LOSS(-)_2_1", 106, 125),
          ColumnConfig("BROKER COMMISSION_2", 125, 143),
          ColumnConfig("OTHER FEES_2", 143, 164),
          ColumnConfig("INT SOLD_2", 164, 179),
          ColumnConfig("DEFERRED INCOME_2", 179, 198)
        )
      ),
      TableRowConfig(
        Seq(
          ColumnConfig("STL DATE", 0, 8),
          ColumnConfig("SECURITY NAME_1", 8, 44),
          ColumnConfig("PRINCIPAL COST_1", 61, 84),
          ColumnConfig("REALISED PROFIT/LOSS(-)_1_2", 106, 125),
          //Copy the values as BASE CCY
          ColumnConfig("PRINCIPAL COST_2", 61, 84),
          ColumnConfig("REALISED PROFIT/LOSS(-)_2_2", 106, 125)
        )
      ),
      TableRowConfig(
        Seq(
          ColumnConfig("SECURITY NAME_2", 8, 44)
        ),
        true
      ),
      TableRowConfig(
        Seq(
          ColumnConfig("SECURITY ID & CODE", 0, 106)
        )
      )
    ),
    "NUMBER OF SHARES :.*"
  )

  val sanitizeConfig = SanitizerConfig(
    ignores = Seq(
      SanitizerLineConfig("PRICE IS OUTSIDE TOLERANCE", true),
      SanitizerLineConfig("SYSTEM DATE"),
      SanitizerLineConfig("TOTAL REALISED PROFIT EXCLUDING ANY REALISED LOSS")
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
      "NET PROCEEDS_1",
      "COST WRITTEN-OFF_1",
      "REALISED PROFIT/LOSS(-)_1_1",
      "BROKER COMMISSION_1",
      "OTHER FEES_1",
      "INT SOLD_1",
      "DEFERRED INCOME_1",
      "PRINCIPAL COST_1",
      "REALISED PROFIT/LOSS(-)_1_2",
      "CCY_2",
      "UNIT PRICE_2",
      "NET PROCEEDS_2",
      "COST WRITTEN-OFF_2",
      "REALISED PROFIT/LOSS(-)_2_1",
      "BROKER COMMISSION_2",
      "OTHER FEES_2",
      "INT SOLD_2",
      "DEFERRED INCOME_2",
      "PRINCIPAL COST_2",
      "REALISED PROFIT/LOSS(-)_2_2",
      "P/L ON EX.RATE"
    )
  )
}