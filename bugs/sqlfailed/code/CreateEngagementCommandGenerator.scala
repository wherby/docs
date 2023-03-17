class CreateEngagementCommandGenerator @Inject() (ServerEnvironment:String, MySQLuser:String, MySQLpwd:String, MySQLipAdress:String, MySQLport:String) extends CommandGeneratorByServerEnvironment {
  val profile: JdbcProfile = _root_.slick.jdbc.MySQLProfile
  import profile.api._
  def generate(): SqlAction[Int, NoStream, Effect] = {
    val databaseName  = if (ServerEnvironment == "SOE") "cidrprod" else "cidr"
    sqlu"""
          CREATE TABLE `V_Engagement_#$ServerEnvironment` (
             `SchemaName` VARCHAR(128) DEFAULT NULL,
            `EngagementId` VARCHAR(128) DEFAULT NULL,
            `Name` VARCHAR(512) DEFAULT NULL,
            `ClientId` VARCHAR(128) DEFAULT NULL,
            `PeriodBeginDate` DATETIME DEFAULT NULL,
            `PeriodEndDate` DATETIME DEFAULT NULL,
            `ReportSignDate` DATETIME DEFAULT NULL,
            `ReportReleaseDate` DATETIME DEFAULT NULL,
            `ArchiveDate` DATETIME DEFAULT NULL,
            `CreateDate` DATETIME DEFAULT NULL,
            `ActualArchiveDate` DATETIME DEFAULT NULL,
            `FreezeDate` DATETIME DEFAULT NULL,
            `TFOffice` VARCHAR(512) DEFAULT NULL,
            `TFRegion` VARCHAR(512) DEFAULT NULL,
            `TFStatusid` VARCHAR(128) DEFAULT NULL,
            `LastUpdateDate` DATETIME DEFAULT NULL,
            `EnableAutoSync` TINYINT(1) NOT NULL DEFAULT '1',
            UNIQUE KEY `V_Engagement_EngagementId_IDX` (`EngagementId`),
            KEY `V_Engagement_LastUpdated_IDX` (`LastUpdateDate`),
            KEY `V_Engagement_Name_IDX` (`Name`),
            KEY `V_Engagement_TFStatusid_IDX` (`TFStatusid`)
          )  ENGINE=FEDERATED DEFAULT CHARSET=UTF8MB4 CONNECTION='mysql://#$MySQLuser:#$MySQLpwd@#$MySQLipAdress:#$MySQLport/#$databaseName/V_Engagement';
          """
  }

}