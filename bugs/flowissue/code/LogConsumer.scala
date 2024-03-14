package com.pwc.ds.cidr.event

import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import cats.implicits._
import com.pwc.ds.cidr.db._
import com.pwc.ds.cidr.db.impl.DbTables
import com.pwc.ds.cidr.models.{BaseQuery, FilterItem}
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import slick.jdbc.JdbcBackend.Database

import java.sql.Date
import java.text.SimpleDateFormat
import javax.inject.Inject
import scala.collection.immutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

abstract class LogConsumer(db: Database)(implicit ec: ExecutionContext, actorSystem: ActorSystem)
  extends DbTables {

  import profile.api._

  trait Processor {
    def process(pre: Option[AuditLogRow], cur: AuditLogRow): Future[Boolean]
  }

  protected def auditLogRowGenerator(offset: Long, batchSize: Int): Future[Seq[AuditLogRow]] =
    db.run(sql"select * from audit_log where id > ${offset} limit ${batchSize}".as[AuditLogRow]).map(_.toSeq)
      .recover[Seq[AuditLogRow]] {
        case _: Throwable => Seq.empty
      }

  private def checkPointRowGenerator(businessName: String): Future[Option[CheckPointRow]] =
    db.run(sql"select * from check_point where `group` = ${businessName}".as[CheckPointRow]).map(_.headOption)
      .recover[Option[CheckPointRow]] {
        case _: Throwable => None
      }

  //  def consume[PT](businessName: String, batchSize: Int): Future[(Option[CheckPointRow], immutable.Seq[AuditLogRow])] = Future {
  //    (None, immutable.Seq.empty[AuditLogRow])
  //  }

  private def commit(businessName: String, offset: Long) =
    db.run(
        sqlu"""
                 INSERT INTO check_point (`group`, snapshot, offset, create_at, modify_at) VALUES ($businessName, "{}", $offset, CURTIME(), CURTIME())
                 ON DUPLICATE KEY UPDATE offset = $offset, modify_at = CURTIME();
    """)
      .recover[Int] {
        case _: Throwable => 0
      }

  def logFilter: AuditLogRow => Boolean

  def businessName: String

  def processor: Processor

  final protected def getDate(timestamp: java.sql.Timestamp) = new Date(timestamp.getTime)

  //  implicit  val actorsystem: ActorSystem
  private def toRun() = {
    val runnable = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      var lastOffset = 0L
      var lastRow: Option[AuditLogRow] = None

      // get the previous record
      def getLast(offset: Long, maxRetry: Int = 100): Future[Option[AuditLogRow]] = {
        if (maxRetry == 0) {
          LoggerFactory.getLogger(this.getClass).warn("getLast row error, pls fix by manual, info: " + businessName)
          return Future.successful(None)
        }
        if (offset <= 0) Future.successful(None)
        else auditLogRowGenerator(offset, 1).flatMap {
          rows => if (rows.isEmpty) getLast(offset - 1, maxRetry - 1) else Future.successful(rows.headOption)
        }
      }

      val initOffsetSource = Source.future(checkPointRowGenerator(businessName))
        .map(_.map(_.offset))
        .map(_.getOrElse(0L))
        .map { s => lastOffset = s; s }
        .mapAsync(1)(s => getLast(s - 1).map { res => lastRow = res; s })
      val concat = b.add(Concat[Long]())
      val bcast = b.add(Broadcast[Long](2))
      val auditLogRowsFlow = Flow[Long].mapAsync(1)(offset => auditLogRowGenerator(offset, 100))
        .throttle(1000, 60.seconds, ele => if (ele.isEmpty) 1000 else 1)
      //        .map { s => println(s"${s.headOption} -- ${s.length} --- ${s.headOption.map(_.id)}"); s }
      val processFlow = Flow[Seq[AuditLogRow]]
        .mapAsync(1) { auditLogRows =>
          //          println(s"occur1 -> $auditLogRows")
          val sinker = Sink.ignore
          RunnableGraph.fromGraph(GraphDSL.createGraph(sinker) { implicit b =>
            sinker =>
              import GraphDSL.Implicits._
              val zip = b.add(ZipWith[Option[AuditLogRow], AuditLogRow, (Option[AuditLogRow], AuditLogRow)]((pre, cur) => (pre, cur)))
              val flow = Flow[(Option[AuditLogRow], AuditLogRow)]
                //                .map { s => println(s"occur preprocess -> $s"); s }
                .mapAsync(1) {
                  case (pre, cur) =>
                    processor.process(pre, cur)
                      .map(_ => cur)
                }.log("process flow error: ")
              val concat = b.add(Concat[Option[AuditLogRow]](2))
              val bcast = b.add(Broadcast[AuditLogRow](2))
              Source.single(lastRow) ~> concat ~> zip.in0
              Source(auditLogRows.toList) ~> Flow[AuditLogRow].filter(logFilter) ~> zip.in1
              zip.out ~> flow ~> bcast
              concat <~ Flow[AuditLogRow].map(Some(_)) <~ bcast
              bcast ~> Flow[AuditLogRow].map { s => lastRow = Some(s); s } ~> sinker
              ClosedShape
          }).run().map(_ => auditLogRows.map(_.id).lastOption)
        }.mapAsync(1) {
          maybeOffset =>
            maybeOffset.foreach(commit(businessName, _))
            maybeOffset.foreach(lastOffset = _)
            Future.successful(lastOffset)
        }

      initOffsetSource ~> concat ~> auditLogRowsFlow.async ~> processFlow ~> bcast ~> Sink.ignore
      concat <~ bcast
      ClosedShape
    })

    runnable.run()
  }

  println("starting to consume, occur -----")
  toRun()

}

object LogConsumer {

  class ProjectCreate @Inject()(db: Database, statisticsDao: StatisticsDao)(implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {
    override def businessName: String = "project_create_committer"

    override def logFilter: AuditLogRow => Boolean = row => row.event == "CREATE PROJECT"

    val item: String = StatisticsIndices.PROJECT_CREATED.replace(' ', '_')
    val itemAgg: String = item + "_AGG"

    override def processor: Processor = new Processor {

      override def process(pre: Option[AuditLogRow], cur: AuditLogRow): Future[Boolean] = {
        //        println(s"${pre.map(_.event)} occur. \t offset: ${pre.map(_.id)}")
        //        println(s"${cur.event} occur. \t offset: ${cur.id}")
        //        println("-------------------------------------------------")
        val maybePreCreateAt = pre.flatMap(_.createAt.map(getDate))
        val curCreateAt: Date = cur.createAt.map(getDate).getOrElse(throw new Exception("audit log has no date, which is necessary"))

        if (maybePreCreateAt.isEmpty) {
          for {
            _ <- statisticsDao.insertIgnore(s"${item}_MIN", curCreateAt.getTime, cur.id, curCreateAt, businessName)
            _ <- statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName)
          } yield true
        } else if (maybePreCreateAt.nonEmpty && curCreateAt.toString != maybePreCreateAt.get.toString) {
          for {
            snap <- statisticsDao.findByKey(itemAgg)
            _ <- Future.traverse(snap.toSeq) { agg =>
              statisticsDao.insertIgnore(item + "_SS_" + agg.computeBefore, agg.result.getOrElse(-1L), cur.id, agg.computeBefore, businessName)
            }
            _ <- statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName)
          } yield true
        } else {
          for {
            _ <- statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName)
          } yield true
        }
      }
    }
  }

  class ProjectDelete @Inject()(db: Database, statisticsDao: StatisticsDao)(implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {
    override def logFilter: AuditLogRow => Boolean = row => row.event == "DELETE PROJECT"

    override def businessName: String = "project_delete_committer"

    val item: String = StatisticsIndices.PROJECT_DELETED.replace(' ', '_')
    val itemAgg: String = item + "_AGG"

    override def processor: Processor = (pre: Option[AuditLogRow], cur: AuditLogRow) => {
      val maybePreCreateAt = pre.flatMap(_.createAt.map(getDate))
      val curCreateAt: Date = cur.createAt.map(getDate).getOrElse(throw new Exception("audit log has no date, which is necessary"))
      if (maybePreCreateAt.isEmpty) {
        for {
          _ <- statisticsDao.insertIgnore(s"${item}_MIN", curCreateAt.getTime, cur.id, curCreateAt, businessName)
          _ <- statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName)
        } yield true
      } else if (maybePreCreateAt.nonEmpty && curCreateAt.toString != maybePreCreateAt.get.toString) {
        for {
          snap <- statisticsDao.findByKey(itemAgg)
          _ <- Future.traverse(snap.toSeq) { agg =>
            statisticsDao.insertIgnore(item + "_SS_" + agg.computeBefore, agg.result.getOrElse(-1L), cur.id, agg.computeBefore, businessName)
          }
          _ <- statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName)
        } yield true
      } else {
        for {
          _ <- statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName)
        } yield true
      }
    }
  }

  class ProjectProcessComplete @Inject()(db: Database, statisticsDao: StatisticsDao, statisticsKVDao: StatisticsKVDao)
                                        (implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {

    val item: String = StatisticsIndices.PROJECT_PROCESS_COMPLETED.replace(' ', '_')
    val itemAgg: String = item + "_AGG"

    override def logFilter: AuditLogRow => Boolean = row => row.event == "COMPLETE PROCESSING PROJECT"

    override def businessName: String = "project_complete_committer"

    override def processor: Processor = (pre: Option[AuditLogRow], cur: AuditLogRow) => {
      val maybePreCreateAt = pre.flatMap(_.createAt.map(getDate))
      val curCreateAt: Date = cur.createAt.map(getDate).getOrElse(throw new Exception("audit log has no date, which is necessary"))
      if (maybePreCreateAt.nonEmpty && curCreateAt.toString != maybePreCreateAt.get.toString) {
        for {
          snap <- statisticsDao.findByKey(itemAgg)
          _ <- Future.traverse(snap.toSeq) { agg =>
            statisticsDao.insertIgnore(item + "_SS_" + agg.computeBefore, agg.result.getOrElse(-1L), cur.id, agg.computeBefore, businessName)
          }
          nums <- statisticsKVDao.insertIgnore(s"${item}_${cur.projectId}", None, cur.id)
          _ <- if (nums > 0) statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName) else Future(0)
        } yield true
      } else {
        for {
          nums <- statisticsKVDao.insertIgnore(s"${item}_${cur.projectId}", None, cur.id)
          _ <- if (nums > 0) statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName) else Future(0)
        } yield true
      }
    }
  }

  class ProjectProofread @Inject()(db: Database, statisticsDao: StatisticsDao, statisticsKVDao: StatisticsKVDao)
                                  (implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {

    val item: String = StatisticsIndices.PROJECT_PROOFREAD
    val itemAgg: String = item + "_AGG"

    override def logFilter: AuditLogRow => Boolean = row => row.event == "RETRIEVED REVIEW DATA"

    override def businessName: String = "project_proofread_committer"

    override def processor: Processor = (pre: Option[AuditLogRow], cur: AuditLogRow) => {
      val maybePreCreateAt = pre.flatMap(_.createAt.map(getDate))
      val curCreateAt: Date = cur.createAt.map(getDate).getOrElse(throw new Exception("audit log has no date, which is necessary"))

      def insertSnapshot = for {
        snap <- statisticsDao.findByKey(itemAgg)
        _ <- Future.traverse(snap.toSeq) { agg =>
          statisticsDao.insertIgnore(item + "_SS_" + agg.computeBefore, agg.result.getOrElse(-1L), cur.id, agg.computeBefore, businessName)
        }
      } yield true

      def aggregate = for {
        nums <- statisticsKVDao.insertIgnore(s"${item}_${cur.projectId}", None, cur.id)
        _ <- if (nums > 0) statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName) else Future(0)
      } yield true

      for {
        _ <- if (maybePreCreateAt.nonEmpty && maybePreCreateAt.get.toString != curCreateAt.toString) insertSnapshot else Future(true)
        _ <- aggregate
      } yield true
    }
  }

  class ProjectExport @Inject()(db: Database, statisticsDao: StatisticsDao, statisticsKVDao: StatisticsKVDao)
                               (implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {

    val item: String = StatisticsIndices.PROJECT_EXPORT.replace(' ', '_')
    val itemAgg: String = item + "_AGG"

    override def logFilter: AuditLogRow => Boolean = row => row.event == "CHANGE PROJECT STATUS" && row.message.contains("Export")

    override def businessName: String = "project_export_committer"

    override def processor: Processor = (pre: Option[AuditLogRow], cur: AuditLogRow) => {
      val maybePreCreateAt = pre.flatMap(_.createAt.map(getDate))
      val curCreateAt: Date = cur.createAt.map(getDate).getOrElse(throw new Exception("audit log has no date, which is necessary"))
      if (maybePreCreateAt.nonEmpty && curCreateAt.toString != maybePreCreateAt.get.toString) {
        for {
          snap <- statisticsDao.findByKey(itemAgg)
          _ <- Future.traverse(snap.toSeq) { agg =>
            statisticsDao.insertIgnore(item + "_SS_" + agg.computeBefore, agg.result.getOrElse(-1L), cur.id, agg.computeBefore, businessName)
          }
          nums <- statisticsKVDao.insertIgnore(s"${item}_${cur.projectId}", None, cur.id)
          _ <- if (nums > 0) statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName) else Future(0)
        } yield true
      } else {
        for {
          nums <- statisticsKVDao.insertIgnore(s"${item}_${cur.projectId}", None, cur.id)
          _ <- if (nums > 0) statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName) else Future(0)
        } yield true
      }
    }
  }


  class ProjectDownloadLabor @Inject()(db: Database, statisticsDao: StatisticsDao, statisticsKVDao: StatisticsKVDao)
                                      (implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {

    val item: String = StatisticsIndices.PROJECT_DOWNLOAD
    val itemAgg: String = item + "_AGG"

    override def logFilter: AuditLogRow => Boolean = row => row.event == "DOWNLOAD LABOUR"

    override def businessName: String = "project_download_committer"

    override def processor: Processor = (pre: Option[AuditLogRow], cur: AuditLogRow) => {
      val maybePreCreateAt = pre.flatMap(_.createAt.map(getDate))
      val curCreateAt: Date = cur.createAt.map(getDate).getOrElse(throw new Exception("audit log has no date, which is necessary"))

      def insertSnapshot = for {
        snap <- statisticsDao.findByKey(itemAgg)
        _ <- Future.traverse(snap.toSeq) { agg =>
          statisticsDao.insertIgnore(item + "_SS_" + agg.computeBefore, agg.result.getOrElse(-1L), cur.id, agg.computeBefore, businessName)
        }
      } yield true

      def aggregate = for {
        nums <- statisticsKVDao.insertIgnore(s"${item}_${cur.projectId}", None, cur.id)
        _ <- if (nums > 0) statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName) else Future(0)
      } yield true

      for {
        _ <- if (maybePreCreateAt.nonEmpty && maybePreCreateAt.get.toString != curCreateAt.toString) insertSnapshot else Future(true)
        _ <- aggregate
      } yield true
    }
  }

  // combine FILE_PROCESSED with ENGAGEMENT_FILE_PROCESSED
  class FileProcessed @Inject()(db: Database, statisticsDao: StatisticsDao, statisticsKVDao: StatisticsKVDao, auditLogDAO: AuditLogDAO)
                               (implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {

    import profile.api._

    val item: String = StatisticsIndices.FILE_PROCESSED.replace(' ', '_')
    val itemAgg: String = item + "_AGG"
    val itemFileUpload = "UPLOAD_FILE"

    val engProjItem: String = StatisticsIndices.ENGAGEMENT_PROJECT.replace(' ', '_')

    def itemEngFileAgg(teid: String) = StatisticsIndices.ENGAGEMENT_FILE_PROCESSED + s"_AGG_$teid"

    def itemEngFileAcc(teid: String) = StatisticsIndices.ENGAGEMENT_FILE_PROCESSED + s"_ACC_$teid"

    def itemEngFileSnapshot(teid: String) = StatisticsIndices.ENGAGEMENT_FILE_PROCESSED + s"_SS_$teid"

    val tagEngFileAcc = StatisticsIndices.ENGAGEMENT_FILE_PROCESSED + "_ACC_"

    // depend on `EngagementProject` task
    override protected def auditLogRowGenerator(offset: Long, batchSize: Int): Future[immutable.Seq[AuditLogRow]] =
      db.run(sql"select * from audit_log where id > $offset and id <= (select offset from check_point where `group` = 'engagement_project_committer') limit $batchSize".as[AuditLogRow]).map(_.toSeq)


    override def logFilter: AuditLogRow => Boolean =
      row => row.event == "UPLOAD FILE" || row.event == "DELETE FILE" || row.event == "COMPLETE PROCESSING PROJECT"

    override def businessName: String = "file-process-committer"

    override def processor: Processor = (pre: Option[AuditLogRow], cur: AuditLogRow) => {
      val maybePreCreateAt = pre.flatMap(_.createAt.map(getDate))
      val curCreateAt: Date = cur.createAt.map(getDate).getOrElse(throw new Exception("audit log has no date, which is necessary"))

      def insertSnapshot = for {
        snap <- statisticsDao.findByKey(itemAgg)
        _ <- Future.traverse(snap.toSeq) { agg =>
          statisticsDao.insertIgnore(item + "_SS_" + agg.computeBefore, agg.result.getOrElse(-1L), cur.id, agg.computeBefore, businessName)
        }
      } yield true

      def insertSnapshotEngFile = for {
        tenantEngIds <- statisticsKVDao.getKeysByKeyLike(tagEngFileAcc).map(_.map(_.replace(tagEngFileAcc, "")))
        _ <- Future.traverse(tenantEngIds) { id =>
          for {
            agg <- statisticsDao.findByKey(itemEngFileAgg(id)).map(_.getOrElse(throw new Exception("must have one, if not, there is one bug")))
            _ <- statisticsDao.insertIgnore(itemEngFileSnapshot(id) + "_" + agg.computeBefore, agg.result.getOrElse(-1L), cur.id, agg.computeBefore, businessName)
          } yield true
        }
        _ <- statisticsKVDao.deleteByKeyLike(tagEngFileAcc)
      } yield true

      def aggregate: Future[Boolean] = cur.event match {
        case "UPLOAD FILE" =>
          for {
            v <- statisticsKVDao.upsert(s"${itemFileUpload}_${cur.projectId}", 1, cur.id)
          } yield true
        case "DELETE FILE" =>
          for {
            v <- statisticsKVDao.upsert(s"${itemFileUpload}_${cur.projectId}", -1, cur.id)
          } yield true
        case "COMPLETE PROCESSING PROJECT" =>
          for {
            maybeRow <- statisticsKVDao.getByKey(s"${itemFileUpload}_${cur.projectId}")
            _ <- maybeRow.traverse { data =>
              println(s"project id ${cur.projectId}, value ${data.value.get}")
              statisticsDao.upsert(itemAgg, Math.max(0L, data.value.get.toLong), cur.id, curCreateAt, businessName)
            }
          } yield true
      }


      def aggregateEngFileProcessed = {
        def fallbackTryToAchieveTenantEngagementId(projectId: Long): Future[Option[String]] = {
          auditLogDAO.list(BaseQuery(Seq(FilterItem("projectId", projectId.toString, dataType = FilterItem.DATATYPE_LONG), FilterItem("event", "CHANGE PROJECT STATUS"), FilterItem("entityType", "class com.pwc.ds.cidr.db.ProjectData"))))
            .map(_.items.headOption).map(_.flatMap(_.newData).map(Json.parse).map(_ \ "tenantEngagementId").map(_.as[String]))
        }

        cur.event match {
          case "COMPLETE PROCESSING PROJECT" =>
            val projectId = cur.projectId
            for {
              maybeRow <- statisticsKVDao.getByKey(s"${itemFileUpload}_${cur.projectId}")
              maybeData <- statisticsKVDao.getByKey(s"${engProjItem /*ENG_PROJ*/}_PID_$projectId")
              maybeTenantEngagementId <- {
                val s: Option[String] = maybeData.flatMap(_.value)
                //              if (s.isEmpty) LoggerFactory.getLogger(this.getClass).warn(s"""lack of "CREATE PROJECT" event for projectId[$projectId], try to fix log by hand""")
                if (s.isEmpty) fallbackTryToAchieveTenantEngagementId(projectId)
                  .map { t =>
                    if (t.isEmpty) {
                      println(s"fallback but can't get tenantEngagementId for project $projectId, count: ${maybeRow.flatMap(_.value).getOrElse("0")}");
                      // break is better than continue
                      throw new Exception(s"fallback but can't get tenantEngagementId for project $projectId, count: ${maybeRow.flatMap(_.value).getOrElse("0")}")
                    } else {
                      println(s"get fallback value $t");
                    }
                    t
                  }
                else Future.successful(s)
              }
              _ <- maybeRow.traverse { data =>
                maybeTenantEngagementId.traverse(teid => {
                  statisticsDao.upsert(itemEngFileAgg(teid), Math.max(0L, data.value.get.toLong), cur.id, curCreateAt, businessName)
                    .flatMap(_ => statisticsKVDao.insertIgnore(itemEngFileAcc(teid), None, cur.id))
                })
              }
            } yield true
          case _ =>
            Future.successful(true)
        }
      }

      for {
        _ <- if (maybePreCreateAt.nonEmpty && curCreateAt.toString != maybePreCreateAt.get.toString) insertSnapshot else Future(true)
        _ <- if (maybePreCreateAt.nonEmpty && curCreateAt.toString != maybePreCreateAt.get.toString) insertSnapshotEngFile else Future(true)
        _ <- aggregate
        _ <- aggregateEngFileProcessed
      } yield true
    }
  }

  class EngagementProject @Inject()(db: Database, statisticsDao: StatisticsDao, statisticsKVDao: StatisticsKVDao, auraDAO: AuraDAO)
                                   (implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {
    val _item: String = StatisticsIndices.ENGAGEMENT_PROJECT.replace(' ', '_')
    val _itemCreate: String = s"${_item}_CREATE"
    val _itemDelete: String = s"${_item}_DELETE"
    val _itemDownload: String = s"${_item}_DOWNLOAD"

    def itemCreateAcc(id: String): String = s"${_itemCreate}_ACC_${id}"

    def itemCreateAgg(id: String): String = s"${_itemCreate}_AGG_${id}"

    def itemCreateSnapshot(id: String): String = s"${_itemCreate}_SS_${id}"


    def itemDeleteAcc(id: String): String = s"${_itemDelete}_ACC_${id}"

    def itemDeleteAgg(id: String): String = s"${_itemDelete}_AGG_${id}"

    def itemDeleteSnapshot(id: String): String = s"${_itemDelete}_SS_${id}"


    def itemDownloadAcc(id: String): String = s"${_itemDownload}_ACC_${id}"

    def itemDownloadAgg(id: String): String = s"${_itemDownload}_AGG_${id}"

    def itemDownloadSnapshot(id: String): String = s"${_itemDownload}_SS_${id}"

    override def logFilter: AuditLogRow => Boolean =
      row => row.event == "CREATE PROJECT" || row.event == "DELETE PROJECT" || row.event == "DOWNLOAD LABOUR"

    override def businessName: String = "engagement_project_committer"

    override def processor: Processor = (pre: Option[AuditLogRow], cur: AuditLogRow) => {
      val maybePreCreateAt = pre.flatMap(_.createAt.map(getDate))
      val curCreateAt: Date = cur.createAt.map(getDate).getOrElse(throw new Exception("audit log has no date, which is necessary"))
      val tagCreateAcc: String = s"${_itemCreate}_ACC_"

      def insertSnapshotCreate = for {
        tenantEngIds <- statisticsKVDao.getKeysByKeyLike(tagCreateAcc).map(_.map(_.replace(tagCreateAcc, "")))
        _ <- Future.traverse(tenantEngIds) { id =>
          for {
            agg <- statisticsDao.findByKey(itemCreateAgg(id)).map(_.getOrElse(throw new Exception("must have one, if not, there is one bug")))
            _ <- statisticsDao.insertIgnore(itemCreateSnapshot(id) + "_" + agg.computeBefore, agg.result.getOrElse(-1L), cur.id, agg.computeBefore, businessName)
          } yield true
        }
        _ <- statisticsKVDao.deleteByKeyLike(tagCreateAcc)
      } yield true

      val tagDeleteAcc: String = s"${_itemDelete}_ACC_"

      def insertSnapshotDelete = for {
        tenantEngIds <- statisticsKVDao.getKeysByKeyLike(tagDeleteAcc).map(_.map(_.replace(tagDeleteAcc, "")))
        _ <- Future.traverse(tenantEngIds) { id =>
          for {
            agg <- statisticsDao.findByKey(itemDeleteAgg(id)).map(_.getOrElse(throw new Exception("must have one, if not, there is one bug")))
            _ <- statisticsDao.insertIgnore(itemDeleteSnapshot(id) + "_" + agg.computeBefore, agg.result.getOrElse(-1L), cur.id, agg.computeBefore, businessName)
          } yield true
        }
        _ <- statisticsKVDao.deleteByKeyLike(tagDeleteAcc)
      } yield true

      val tagDownloadAcc: String = s"${_itemDownload}_ACC_"

      def insertSnapshotDownload = for {
        tenantEngIds <- statisticsKVDao.getKeysByKeyLike(tagDownloadAcc).map(_.map(_.replace(tagDownloadAcc, "")))
        _ <- Future.traverse(tenantEngIds) { id =>
          for {
            agg <- statisticsDao.findByKey(itemDownloadAgg(id)).map(_.getOrElse(throw new Exception("must have one, if not, there is one bug")))
            _ <- statisticsDao.insertIgnore(itemDownloadSnapshot(id) + "_" + agg.computeBefore, agg.result.getOrElse(-1L), cur.id, agg.computeBefore, businessName)
          } yield true
        }
        _ <- statisticsKVDao.deleteByKeyLike(tagDownloadAcc)
      } yield true

      def insertSnapshot = for {
        _ <- insertSnapshotCreate
        _ <- insertSnapshotDelete
        _ <- insertSnapshotDownload
      } yield true

      def aggregate: Future[Boolean] = cur.event match {
        case "CREATE PROJECT" =>
          val maybeEngagementId: Option[String] = cur.newData.map { newData => (Json.parse(newData) \ "tenantEngagementId").as[String] }
          val tenantEngagementId: String = maybeEngagementId.getOrElse(throw new Exception("invalid log data, try to fix by hand"))
          val projectId = cur.projectId

          for {
            _ <- statisticsDao.upsert(itemCreateAgg(tenantEngagementId), 1, cur.id, curCreateAt, businessName)
            _ <- statisticsKVDao.insertIgnore(itemCreateAcc(tenantEngagementId), None, cur.id)
            _ <- statisticsKVDao.insertIgnore(s"${_item}_PID_$projectId", Some(tenantEngagementId), cur.id)
          } yield true
        case "DELETE PROJECT" =>
          val projectId = cur.projectId

          for {
            maybeData <- statisticsKVDao.getByKey(s"${_item}_PID_$projectId")
            maybeTenantEngagementId <- Future.successful {
              val s: Option[String] = maybeData.flatMap(_.value)
              if (s.isEmpty) LoggerFactory.getLogger(this.getClass).warn(s"""lack of "CREATE PROJECT" event for projectId[$projectId], try to fix log by hand""")
              s
            }
            _ <- maybeTenantEngagementId.traverse(tenantEngagementId => statisticsDao.upsert(itemDeleteAgg(tenantEngagementId), 1, cur.id, curCreateAt, businessName))
            _ <- maybeTenantEngagementId.traverse(tenantEngagementId => statisticsKVDao.insertIgnore(itemDeleteAcc(tenantEngagementId), None, cur.id))
          } yield true
        case "DOWNLOAD LABOUR" =>
          val projectId = cur.projectId

          for {
            maybeData <- statisticsKVDao.getByKey(s"${_item}_PID_$projectId")
            maybeTenantEngagementId <- Future.successful {
              val s: Option[String] = maybeData.flatMap(_.value)
              if (s.isEmpty) LoggerFactory.getLogger(this.getClass).warn(s"""lack of "CREATE PROJECT" event for projectId[$projectId], try to fix log by hand""")
              s
            }
            _ <- maybeTenantEngagementId.traverse(tenantEngagementId => statisticsDao.upsert(itemDownloadAgg(tenantEngagementId), 1, cur.id, curCreateAt, businessName))
            _ <- maybeTenantEngagementId.traverse(tenantEngagementId => statisticsKVDao.insertIgnore(itemDownloadAcc(tenantEngagementId), None, cur.id))
          } yield true
      }

      for {
        _ <- if (maybePreCreateAt.nonEmpty && maybePreCreateAt.get.toString != curCreateAt.toString) insertSnapshot else Future(true)
        _ <- aggregate
      } yield true
    }
  }

  class EngagementCreate @Inject()(db: Database, statisticsDao: StatisticsDao, statisticsKVDao: StatisticsKVDao, auraDAO: AuraDAO)
                                  (implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {

    import profile.api._

    val item: String = StatisticsIndices.ENGAGEMENT_CREATE

    val itemAgg: String = item + "_AGG"

    var preOffset: Option[Long] = None

    final def adapter(row: TenantEngagementRow) =
      AuditLogRow(row.createAt.get.getTime, "ENGAGEMENT CREATE", -1, "-1", "", "", message = Some(row.id), createAt = row.createAt)

    override protected def auditLogRowGenerator(offset: Long, batchSize: Int): Future[Seq[AuditLogRow]] = {
      //      println(s"TenantEngagement generate data $offset, batchSize $batchSize")
      val sql = TenantEngagement.filter(_.createAt > new java.sql.Timestamp(offset)).sortBy(_.createAt.asc) /* no limit */.result.map(_.toSeq)
      db.run(sql).map(_.map(adapter))
    }

    override def logFilter: AuditLogRow => Boolean = row => row.event == "ENGAGEMENT CREATE"

    override def businessName: String = "engagement_create_committer"

    override def processor: Processor = (pre: Option[AuditLogRow], cur: AuditLogRow) => {
      val curCreateAt: Date = cur.createAt.map(getDate).getOrElse(throw new Exception("audit log has no date, which is necessary"))

      val tenantEngagementId: String = cur.message.get
      val formatter = new SimpleDateFormat("yyyy-MM-dd")


      def insertEngSnapshot = for {
        maybeAuraEngagement <- auraDAO.lookupByTenantEngagementId(tenantEngagementId)
        _ <- maybeAuraEngagement.flatMap(_.name).traverse(name => statisticsKVDao.insertIgnore(s"${item}_TEID_$tenantEngagementId", Some(name), cur.id))
        _ <- maybeAuraEngagement.flatMap(_.clientName).traverse(clientName => statisticsKVDao.insertIgnore(s"${item}_TEID_CN_$tenantEngagementId", Some(clientName), cur.id))
      } yield true

      def insertItems = for {
        archiveDate <- auraDAO.queryArchiveDate(tenantEngagementId).map(_.map(formatter.format))
        _ <- statisticsKVDao.insertIgnore(s"${item}_${curCreateAt}_$tenantEngagementId", archiveDate, cur.id)
      } yield true

      val maybePreCreateAt = pre.flatMap(_.createAt.map(getDate))

      def insertSnapshot = for {
        snap <- statisticsDao.findByKey(itemAgg)
        _ <- Future.traverse(snap.toSeq) { agg =>
          statisticsDao.insertIgnore(item + "_SS_" + agg.computeBefore, agg.result.getOrElse(-1L), cur.id, agg.computeBefore, businessName)
        }
      } yield true

      def aggregate = for {
        _ <- if (preOffset.nonEmpty && preOffset.get >= cur.id) {
          statisticsDao.upsert(itemAgg, 1, preOffset.get + 1, curCreateAt, businessName)
            .map { _ => preOffset = Some(preOffset.get + 1); true }
        } else {
          statisticsDao.upsert(itemAgg, 1, cur.id, curCreateAt, businessName)
            .map { _ => preOffset = Some(cur.id); true }
        }
      } yield true


      for {
        _ <- insertItems
        // agg func
        _ <- if (maybePreCreateAt.nonEmpty && maybePreCreateAt.get.toString != curCreateAt.toString) insertSnapshot else Future(true)
        _ <- aggregate
        // snapshot engagement name and client name
        _ <- insertEngSnapshot
      } yield true
    }
  }

  class UserActive @Inject()(db: Database, statisticsKVDao: StatisticsKVDao)
                            (implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {
    override def logFilter: AuditLogRow => Boolean = row => row.createBy.nonEmpty

    override def businessName: String = "user_active_committer"

    val item = StatisticsIndices.USER_ACTIVE

    var cache = Map[String, Option[String]]()

    override def processor: Processor = (pre: Option[AuditLogRow], cur: AuditLogRow) => {
      val curCreateAt: Date = cur.createAt.map(getDate).getOrElse(throw new Exception("audit log has no date, which is necessary"))
      val key = s"${item}_${curCreateAt}_${cur.createBy}"
      for {
        _ <- if (!cache.contains(key)) statisticsKVDao.insertIgnore(key, None, cur.id).map { s => cache += (key -> None); s } else Future.successful(0)
      } yield true
    }
  }


  class ContractActive @Inject()(db: Database,
                                 statisticsKVDao: StatisticsKVDao,
                                 packageDao: PackageDao,
                                 contractDao: ContractDao,
                                )
                                (implicit ec: ExecutionContext, actorSystem: ActorSystem) extends LogConsumer(db) {

    import profile.api._

    // depend on `EngagementProject` task
    override protected def auditLogRowGenerator(offset: Long, batchSize: Int): Future[Seq[AuditLogRow]] =
      db.run(sql"select * from audit_log where id > $offset and id <= (select offset from check_point where `group` = 'engagement_project_committer') limit $batchSize".as[AuditLogRow]).map(_.toSeq)

    override def logFilter: AuditLogRow => Boolean = row => row.event == "START PROCESSING PROJECT"

    override def businessName: String = "contract_active_committer"

    val item = StatisticsIndices.CONTRACT_ACTIVE

    val engProjItem: String = StatisticsIndices.ENGAGEMENT_PROJECT

    override def processor: Processor = (pre: Option[AuditLogRow], cur: AuditLogRow) => {
      val curCreateAt: Date = cur.createAt.map(getDate).getOrElse(throw new Exception("audit log has no date, which is necessary"))
      val projectId = cur.projectId
      for {
        packageRow <- packageDao.getByProjectId(projectId)
        maybeTeid <- statisticsKVDao.getByKey(s"${engProjItem}_PID_$projectId").map(_.flatMap(_.value))
        tmpIds <- packageRow.traverse(pkg => contractDao.shallowList(BaseQuery(Seq(FilterItem("packageId", pkg.id.toString))))).map(_.toSeq.flatten).map(_.map(_.contractTemplateId))
        _ <- maybeTeid.traverse(teid => {
          Future.traverse(tmpIds) { tmpId =>
            statisticsKVDao.insertIgnore(s"${item}_${curCreateAt}_${teid}_$tmpId", None, cur.id)
          }
        })
      } yield true
    }
  }
}


object StatisticsIndices {
  // MUSTN'T CHANGE
  val PROJECT_CREATED = "CREATE_PROJECT"
  val PROJECT_DELETED = "DELETE_PROJECT"
  val PROJECT_PROCESS_COMPLETED = "COMPLETE_PROCESSING_PROJECT"
  val PROJECT_PROOFREAD = "PROJECT_PROOFREAD"
  val PROJECT_EXPORT = "PROJECT_EXPORT"
  val PROJECT_DOWNLOAD = "PROJECT_DOWNLOAD"

  val FILE_PROCESSED = "FILE_PROCESSED"

  val ENGAGEMENT_PROJECT = "ENG_PROJ"
  val ENGAGEMENT_FILE_PROCESSED = "ENG_FILE_PROCESSED"

  val ENGAGEMENT_CREATE = "ENGAGEMENT_CREATE"

  val USER_ACTIVE = "USER_ACTIVE"

  val CONTRACT_ACTIVE = "CONTRACT_ACTIVE"
}
