package service

  ...

trait UserRead {
  def lookup(id: String): Future[Option[User]]
}
