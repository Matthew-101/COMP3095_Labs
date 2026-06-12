\echo '=========================================================='
\echo 'Starting PostgreSQL initialization for order-service'
\echo '=========================================================='

-- Step 1: Create a least-privilege application user
CREATE USER order_user WITH PASSWORD 'order_password';

-- Step 2: Grant connection rights to order_service database
GRANT CONNECT ON DATABASE order_service TO order_user;

-- Step 3: Grant usage and create on public schema
GRANT USAGE, CREATE ON SCHEMA public TO order_user;

-- Step 4: GRANT full access to all current tables and sequences
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO order_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO order_user;

-- Step 5: Apply the same privileges to tables/sequences created in future
ALTER DEFAULT PRIVILEGES IN SCHEMA public
      GRANT ALL ON TABLES TO order_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
      GRANT ALL ON SEQUENCES TO order_user;

\echo '=========================================================='
\echo 'PostgreSQL initialization completed successfully'
\echo '=========================================================='