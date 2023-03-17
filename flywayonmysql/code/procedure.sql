DELIMITER $$
DROP PROCEDURE IF EXISTS `createUserFundForFund` $$
CREATE PROCEDURE `createUserFundForFund` (fiId VARCHAR(256))
BEGIN
  drop temporary table if exists invResult;
 ...
  insert into userfunds (id, userid, fundid, active, deleted, createdatetime, meta) select * from newUserfunds;

END $$
DELIMITER ；

DELIMITER $$

DROP PROCEDURE IF EXISTS `createUserFundForAllFunds` $$
CREATE PROCEDURE `createUserFundForAllFunds`()
BEGIN

...

END $$

DELIMITER ;

CALL createUserFundForAllFunds();
