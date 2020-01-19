package service

trait UserWrite {
  def create(user: User): Future[Int]
}
