@Singleton
class SlickStatDAO @Inject()(db: Database)(implicit ec: ExecutionContext) extends StatDAO with Tables {


  val usedb = new UseDBcommandGenerator("idr_usage").generate()

  /**
   * for HK
   */

...

  val drop_v_engagement_HK = new DropEngagementCommandGenerator("HK").generate()
  val create_v_engagement_HK = new CreateEngagementCommandGenerator("HK", mysqlHKUser, mysqlHKPwd, mysqlHkIpAddress, mysqlHKPort).generate()
  val drop_tenant_HK = new DropTenantCommandGenerator("HK").generate()
  val create_tenant_HK = new CreateTenantCommandGenerator("HK", mysqlHKUser, mysqlHKPwd, mysqlHkIpAddress, mysqlHKPort).generate()
  val drop_tenant_engagement_HK = new DropTenantEngagementCommandGenerator("HK").generate()
  val create_tenant_engagement_HK = new CreateTenantEngagementCommandGenerator("HK", mysqlHKUser, mysqlHKPwd, mysqlHkIpAddress, mysqlHKPort).generate()
  val drop_audit_log_HK = new DropAuditLogCommandGenerator("HK").generate()
  val create_audit_log_HK = new CreateAuditLogCommandGenerator("HK", mysqlHKUser, mysqlHKPwd, mysqlHkIpAddress, mysqlHKPort).generate()
  val drop_Local_Engagement_HK = new DropLocalEngagementCommandGenerator("HK").generate()
  val create_Local_Engagement_HK = new CreateLocalEngagementCommandGenerator("HK").generate()
  val insert_into_Local_Engagement_HK = new InsertIntoLocalEngagementCommandGenerator("HK").generate()
  //    val alter_Environment_HK = new AlterLocalEngagementCommandGenerator("HK", "Environment", "HK-AMIR").generate()
  val alter_Environment_HK = new AlterLocalEngagementCommandGenerator("HK", "Environment", "Hong Kong").generate()


  /**
   * for common
   */
...

  /**
   * UNION
   */
..

  override def getAll: Future[Seq[Stat]] = {
    val sql = IdrUsageUnion.result
    db.run(sql).map(_.map(statsRowToStat))
  }
  override def getAllWOid: Future[Seq[StatWOid]] = {
    val sql = IdrUsageUnion.result
    db.run(sql).map(_.map(statsRowToStatWOid))
  }

  def compute: Future[Seq[Stat]] = this.synchronized {

    val f = for {
      _ <- db.run(usedb)
//      HK
      _ <- db.run(drop_v_engagement_HK)
      _ <- db.run(create_v_engagement_HK)
      _ <- db.run(drop_tenant_HK)
      _ <- db.run(create_tenant_HK)
      _ <- db.run(drop_tenant_engagement_HK)
      _ <- db.run(create_tenant_engagement_HK)
      _ <- db.run(drop_audit_log_HK)
      _ <- db.run(create_audit_log_HK)
      _ <- db.run(drop_Local_Engagement_HK)
      _ <- db.run(create_Local_Engagement_HK)
      _ <- db.run(insert_into_Local_Engagement_HK)
      _ <- db.run(alter_Environment_HK)
//      COMMON
...


      /**
       * Union all Local Engagement Tables
       */
      _ <- db.run(drop_union_all)
      _ <- db.run(create_union_all)
      x <- db.run(IdrUsageUnion.result)
    } yield x

    f.map(seq => seq.map(statsRowToStat))
  }

}
