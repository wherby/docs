package service.impl


@Singleton
class UserWriteImpl @Inject()(userDAO: UserDAO)(implicit ec: ExecutionContext) extends UserWrite {
  override def create(user: User): Future[Int] = {
    userDAO.create(user)
  }
}
