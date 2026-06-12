\echo '=================================================='
\echo 'Starting PostgreSQL initialization for inventory-service...'
\echo '=================================================='

-- -----------------------------------------------------------
-- Step 1: Create a least-privilege application user
-- -----------------------------------------------------------
CREATE USER inventory_user WITH PASSWORD 'inventory_password';

GRANT CONNECT ON DATABASE inventory_service TO inventory_user;

GRANT USAGE, CREATE ON SCHEMA public TO inventory_user;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO inventory_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO inventory_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON TABLES TO inventory_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON SEQUENCES TO inventory_user;

\echo '=================================================='
\echo 'PostgreSQL initialization completed successfully.'
\echo '=================================================='
