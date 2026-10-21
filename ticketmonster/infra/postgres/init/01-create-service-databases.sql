CREATE USER catalog_user WITH PASSWORD 'catalog_password';
CREATE USER inventory_user WITH PASSWORD 'inventory_password';
CREATE USER booking_user WITH PASSWORD 'booking_password';
CREATE USER telemetry_user WITH PASSWORD 'telemetry_password';

CREATE DATABASE catalog_db OWNER catalog_user;
CREATE DATABASE inventory_db OWNER inventory_user;
CREATE DATABASE booking_db OWNER booking_user;
CREATE DATABASE telemetry_db OWNER telemetry_user;

\connect catalog_db
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS catalog AUTHORIZATION catalog_user;
ALTER DATABASE catalog_db SET search_path TO catalog, public;

\connect inventory_db
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS inventory AUTHORIZATION inventory_user;
ALTER DATABASE inventory_db SET search_path TO inventory, public;

\connect booking_db
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS booking AUTHORIZATION booking_user;
ALTER DATABASE booking_db SET search_path TO booking, public;

\connect telemetry_db
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS telemetry AUTHORIZATION telemetry_user;
ALTER DATABASE telemetry_db SET search_path TO telemetry, public;
