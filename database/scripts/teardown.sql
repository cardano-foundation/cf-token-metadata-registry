REVOKE CONNECT ON DATABASE :"cf_metadata_dbname" FROM :"cf_metadata_serviceuser_name";
SELECT pid, pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE datname = :'cf_metadata_dbname' AND pid <> pg_backend_pid();
DROP DATABASE IF EXISTS :"cf_metadata_dbname";
DROP USER IF EXISTS :"cf_metadata_serviceuser_name";