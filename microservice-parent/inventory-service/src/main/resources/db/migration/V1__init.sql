CREATE TABLE t_inventory (
    id       BIGSERIAL    NOT NULL,
    sku_code VARCHAR(255),
    quantity INT,
    PRIMARY KEY (id)
);
