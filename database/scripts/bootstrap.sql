CREATE DATABASE :"cf_metadata_dbname";
CREATE USER :"cf_metadata_serviceuser_name" WITH PASSWORD :'cf_metadata_serviceuser_secret';
GRANT CONNECT ON DATABASE :"cf_metadata_dbname" TO :"cf_metadata_serviceuser_name";