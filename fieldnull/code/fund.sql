create table `funds`(
   `id` varchar(128) NOT NULL,
   `name` VARCHAR(128) not null,
    fund_admin_id varchar(128) null,
   `fund_type` VARCHAR(128) not null,
   `legal_structure` VARCHAR(128) not null,
   `base_currency` VARCHAR(128) not null,
   `audit_period_begin` TIMESTAMP not null,
   `audit_period_end` TIMESTAMP not null,
   `fund_admin` VARCHAR(128) not null,
   `admin_code` VARCHAR(128) NULL,
   `createby` VARCHAR(128) NULL,
   `createdatetime`  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL,
   `modifyby` VARCHAR(128) NULL,
   `modifydatetime`  TIMESTAMP  NULL,
   PRIMARY KEY (`id`)
)
