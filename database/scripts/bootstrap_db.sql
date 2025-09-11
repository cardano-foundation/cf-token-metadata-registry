-- The database is created automatically by POSTGRES_DB env variable
-- Only create user if it doesn't exist (postgres will be the default user)
-- Grant all privileges on the database to the user
GRANT ALL PRIVILEGES ON DATABASE cf_token_metadata_registry TO cardano;