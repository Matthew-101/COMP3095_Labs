-- =============================================================
-- Flyway Migration V2 — Seed inventory data
-- =============================================================
-- Populates t_inventory with a representative set of products
-- so the service is immediately testable after startup.
--
-- Run order: applied once, after V1__init.sql creates the table.
-- To reset: docker compose down -v && docker compose up -d
-- =============================================================

INSERT INTO t_inventory (sku_code, quantity) VALUES
    ('samsung_tv_2025',    100),
    ('iphone_15',           50),
    ('macbook_pro_2025',    25),
    ('sony_headphones',     75),
    ('lg_monitor_27',       40),
    ('ipad_pro_2025',       30),
    ('samsung_galaxy_s25',  60),
    ('dell_laptop_xps15',   15),
    ('logitech_mx_keys',   200),
    ('out_of_stock_item',    0);