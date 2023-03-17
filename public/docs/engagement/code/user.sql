create table `users`(
   `id` varchar(128) NOT NULL,
   `email` VARCHAR(128) NOT NULL,
   `role` VARCHAR(128) NOT NULL,
   `engagementid` TEXT NULL,
   PRIMARY KEY (`id`),
   CONSTRAINT `email` UNIQUE (`email`)
)
