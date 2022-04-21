--liquibase formatted sql
				
--changeset cf:2
create table "wallet_scam_lookup" (
	"wallet_hash" varchar(64),
	"scam_incident_id" bigint,
  "domain" varchar(32),
  "reported" timestamp,
  primary key ("wallet_hash", "scam_incident_id")
);
--rollback drop table "wallet_scam_lookup" cascade;
