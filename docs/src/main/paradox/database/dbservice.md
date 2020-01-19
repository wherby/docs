# DB service


In traditional way, the database service will be implemented in this way:

Controller
: @@snip [UserController.scala](/docs/src/main/scala/controller/UserController.scala)

Database
: @@snip [UserDAO.scala](/docs/src/main/scala/db/UserDAO.scala)

Database impl
: @@snip [SlickUserDAO.scala](/docs/src/main/scala/db/impl/SlickUserDAO.scala)

Read

: @@snip [UserRead.scala](/docs/src/main/scala/service/UserRead.scala)

Read Impl

: @@snip [UserReadImpl.scala](/docs/src/main/scala/service/impl/UserReadImpl.scala)

Write

: @@snip [UserWrite.scala](/docs/src/main/scala/service/UserWrite.scala)

Write Impl

: @@snip [UserWriteImpl.scala](/docs/src/main/scala/service/impl/UserWriteImpl.scala)

Module

: @@snip [Module.scala](/docs/src/main/scala/Module.scala)


