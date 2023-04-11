--liquibase formatted sql
				
--changeset cf:3
create table "sync_control" (
	"lock" char(1) not null,
	"registry_hash" varchar(64) not null,
	"updated" timestamp not null,
	constraint "SYNC_CONTROL_PK_T1" PRIMARY KEY ("lock"),
    constraint "SYNC_CONTROL_CK_T1_Locked" CHECK ("lock"='X')
);
--rollback drop table "sync_control" cascade;