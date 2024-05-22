create table `engagements`(
	`id` varchar(128) NOT NULL,
	`engagementcode` varchar(128) NOT NULL,
    `name` varchar(128) NOT NULL,
    `periodstart` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL,
    `periodend` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL,
    `auralink` varchar(128),
    `active` BOOLEAN DEFAULT TRUE NOT NULL,
    `deleted` varchar(128) DEFAULT 'false' NOT NULL,
    `createdatetime`  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL,
    `meta` TEXT NULL,
    KEY `deleted` (`deleted`),
    key `engagementcode` (`engagementcode`),
    Key  `name` (`name`),
    key `auralink` (`auralink`),
    key `active` (`active`),
     CONSTRAINT engagementcodenameperiod UNIQUE (engagementcode, name,periodstart,deleted),
     CONSTRAINT `id` UNIQUE (`id`),
    PRIMARY KEY (`id`)
)