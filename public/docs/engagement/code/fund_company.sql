CREATE TABLE fund_company_investments (
    id varchar(128) NOT NULL, 
    fund_id varchar(255) NOT NULL,
    company_id varchar(128) NOT NULL, 
    year INT NOT NULL,
    PRIMARY KEY (id)
);