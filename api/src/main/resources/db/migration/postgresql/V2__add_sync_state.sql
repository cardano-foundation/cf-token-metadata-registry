create table "sync_state" (
    "id"               bigserial   primary key,
    "last_commit_hash" varchar(40) not null
);

--rollback drop table "sync_state" cascade;
