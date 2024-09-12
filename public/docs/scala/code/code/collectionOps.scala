/*
 * Copyright (C) 2024 [Tao Zhou](187225577@qq.com). - All Rights Reserved
 *
 * Unauthorized copying or redistribution of this file in source and binary forms via any medium
 * is strictly prohibited.
 */

package inceptapp.component

import inception.const.Ops
import inception.message.CollectionMsg
import inception.service.record.RecordCollectionService
import play.api.libs.json.Json

import scala.concurrent.Future

object CollectionOpsHelper {
  import inception.component.BlockExecutionContext.ex
  import inception.component.CoreJsonFormat._

  def handleCollectionOps(
      collectionMsg: CollectionMsg,
      recordCollectionService: RecordCollectionService
  ): Future[String] = {
    def fxn: ((CollectionMsg, RecordCollectionService) => Future[String]) => Future[String] =
      fn(collectionMsg, recordCollectionService)(_)
    collectionMsg.collectionCmd.op match {
      case Ops.Read   => fxn(handleRead)
      case Ops.Update => fxn(handleUpdate)
      case Ops.Delete => fxn(handleDelete)
    }
  }

  private def fn(collectionMsg: CollectionMsg, recordCollectionService: RecordCollectionService)(
      fx: (CollectionMsg, RecordCollectionService) => Future[String]
  ): Future[String] = {
    fx(collectionMsg, recordCollectionService)
  }

  def handleRead(collectionMsg: CollectionMsg, recordCollectionService: RecordCollectionService) = {
    recordCollectionService
      .handleRead(
        collectionMsg.indexNameSpace.toNamespace,
        collectionMsg.collectionCmd.idx,
        collectionMsg.collectionCmd.payload
      )
      .map { res =>
        Json.toJson(res).toString()
      }
  }

  def handleUpdate(
      collectionMsg: CollectionMsg,
      recordCollectionService: RecordCollectionService
  ) = {
    val key = collectionMsg.collectionCmd.payload.key
      .getOrElse(CNaming.timebasedName(collectionMsg.collectionCmd.idx))

    val payload = collectionMsg.collectionCmd.payload.copy(key = Some(key))
    recordCollectionService
      .handleUpdate(
        collectionMsg.indexNameSpace.toNamespace,
        collectionMsg.collectionCmd.idx,
        payload
      )
      .map(_.toString)
  }

  def handleDelete(
      collectionMsg: CollectionMsg,
      recordCollectionService: RecordCollectionService
  ) = {
    recordCollectionService
      .handleDelete(
        collectionMsg.indexNameSpace.toNamespace,
        collectionMsg.collectionCmd.idx,
        collectionMsg.collectionCmd.payload
      )
      .map(_.toString)
  }
}