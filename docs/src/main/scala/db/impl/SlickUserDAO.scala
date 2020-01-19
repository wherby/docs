package db.impl

class SlickUserDAO @Inject()(db: Database)(implicit ec: ExecutionContext) extends UserDAO with DbTables {

  import profile.api._

  override def lookup(id: String): Future[Option[User]] = {
    val f: Future[Option[UsersRow]] = db.run(queryById(id).result.headOption)
    f.map { maybeRow => maybeRow.map(usersRowToUser) }
  }

  override def create(user: User): Future[Int] = {
    db.run(
      Users += userToUsersRow(user)
    )
  }

  ...
}

