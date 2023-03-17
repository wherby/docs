create table `fund_engagement_report_type_selection`(
	`id` varchar(128) NOT NULL,
    `sheettypes_reporttypes_map_id` varchar(128) NOT NULL,
...
    `modifyby` VARCHAR(128) NULL,
    `modifydatetime`  TIMESTAMP  NULL,
    `selected` boolean NOT NULL DEFAULT FALSE,
    `upload_file_content` MEDIUMBLOB NULL,
    `extraction_file_content` MEDIUMBLOB NULL,
...
);