--liquibase formatted sql
				
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
