--liquibase formatted sql logicalFilePath:metadata.changelog-1.0.sql

--changeset cf:1
create table "metadata" (
	"subject" varchar(255) primary key,
	"policy" text,
  "name" varchar(255),
  "ticker" varchar(32),
  "url" varchar(255),
  "description" text,
  "decimals" integer,
	"updated" timestamp,
	"updated_by" varchar(255),
  "properties" jsonb
);
--rollback drop table "metadata" cascade;

alter table "metadata" add column "textsearch" tsvector 
    generated always as
        (setweight(to_tsvector('english', coalesce("name", '')), 'A') ||
         setweight(to_tsvector('english', coalesce("ticker", '')), 'A') ||
         setweight(to_tsvector('english', coalesce("description", '')), 'B') ||
         setweight(to_tsvector('english', coalesce("url", '')), 'C') ||
         setweight(to_tsvector('english', coalesce("updated_by", '')), 'C')) stored;

create table "logo" (
  "subject" varchar(255) primary key,
  "logo" text,

  constraint "fk_logo_metadata_subject" foreign key("subject") references "metadata"("subject")
);
--rollback drop table "logo" cascade;

create index "idx_metadata_defaultfields" on "metadata"("subject", "policy", "name", "ticker", "url", "description", "decimals", "updated" desc, "updated_by");
--rollback drop index "idx_metadata_defaultfields";

create index "idx_metadata_properties" on "metadata" using gin("properties");
--rollback drop index "idx_metadata_properties";

--liquibase formatted sql logicalFilePath:metadata.changelog-2.0.sql

--changeset cf:2
create table "wallet_scam_lookup" (
	"wallet_hash" varchar(64),
	"scam_incident_id" bigint,
  "domain" varchar(32),
  "reported" timestamp,
  primary key ("wallet_hash", "scam_incident_id")
);
--rollback drop table "wallet_scam_lookup" cascade;

--liquibase formatted sql logicalFilePath:metadata.changelog-3.0.sql

--changeset cf:3
create table "sync_control" (
	"lock" char(1) not null,
	"registry_hash" varchar(64) not null,
	"updated" timestamp not null,
	constraint "SYNC_CONTROL_PK_T1" PRIMARY KEY ("lock"),
    constraint "SYNC_CONTROL_CK_T1_Locked" CHECK ("lock"='X')
);
--rollback drop table "sync_control" cascade;

--liquibase formatted sql logicalFilePath:metadata.changelog-4.0.sql

--changeset cf:4
alter table "metadata" add column "source" varchar(255);
alter table "logo" add column "source" varchar(255);
update "metadata" set "source" = 'cf-mainnet';
update "logo" set "source" = 'mainnet';
alter table "logo" drop constraint "fk_logo_metadata_subject";
alter table "logo" drop constraint "logo_pkey";
alter table "logo" add primary key ("subject", "source");
alter table "metadata" drop constraint "metadata_pkey";
alter table "metadata" add primary key ("subject", "source");
alter table "logo" add constraint "fk_logo_metadata" foreign key("subject", "source") references "metadata"("subject", "source");
drop index if exists "idx_metadata_defaultfields";
create index "idx_metadata_defaultfields" on "metadata"("subject", "source", "policy", "name", "ticker", "url", "description", "decimals", "updated" desc, "updated_by");