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