package service.impl

import scala.concurrent.ExecutionContext

class UserReadImpl @Inject()(userDAO: UserDAO)(implicit ec: ExecutionContext) extends UserRead {
  override def lookup(id: String): Future[Option[User]] = {
    userDAO.lookup(id)
  }
}
