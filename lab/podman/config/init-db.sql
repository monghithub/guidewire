-- =============================================================================
-- init-db.sql — PostgreSQL initialization for Guidewire POC
-- =============================================================================
-- This script runs automatically on first container startup via the
-- /docker-entrypoint-initdb.d/ mechanism. It creates all databases,
-- users, and grants the necessary privileges.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Create users
-- -----------------------------------------------------------------------------
CREATE USER apicurio WITH PASSWORD 'apicurio123';
CREATE USER billing_user WITH PASSWORD 'billing123';
CREATE USER incidents_user WITH PASSWORD 'incidents123';
CREATE USER customers_user WITH PASSWORD 'customers123';

-- -----------------------------------------------------------------------------
-- 2. Create databases with dedicated owners
-- -----------------------------------------------------------------------------
CREATE DATABASE apicurio OWNER apicurio;
CREATE DATABASE billing OWNER billing_user;
CREATE DATABASE incidents OWNER incidents_user;
CREATE DATABASE customers OWNER customers_user;

-- -----------------------------------------------------------------------------
-- 3. Grant privileges (each user can only access their own database)
-- -----------------------------------------------------------------------------
GRANT ALL PRIVILEGES ON DATABASE apicurio TO apicurio;
GRANT ALL PRIVILEGES ON DATABASE billing TO billing_user;
GRANT ALL PRIVILEGES ON DATABASE incidents TO incidents_user;
GRANT ALL PRIVILEGES ON DATABASE customers TO customers_user;

-- Revoke public access to enforce isolation
REVOKE ALL ON DATABASE apicurio FROM PUBLIC;
REVOKE ALL ON DATABASE billing FROM PUBLIC;
REVOKE ALL ON DATABASE incidents FROM PUBLIC;
REVOKE ALL ON DATABASE customers FROM PUBLIC;

-- -----------------------------------------------------------------------------
-- 4. Enable uuid-ossp extension on each service database
-- -----------------------------------------------------------------------------

-- Apicurio database
\connect apicurio
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
GRANT ALL ON SCHEMA public TO apicurio;

-- Billing database
\connect billing
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
GRANT ALL ON SCHEMA public TO billing_user;

-- Incidents database
\connect incidents
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
GRANT ALL ON SCHEMA public TO incidents_user;

-- Customers database
\connect customers
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
GRANT ALL ON SCHEMA public TO customers_user;
