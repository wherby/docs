package db

import scala.concurrent.Future


trait UserDAO {
  def lookup(id: String): Future[Option[User]]

  def create(user: User): Future[Int]
    ...

}


case class User(id: String, email: String)



