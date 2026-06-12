CREATE TABLE t_orders
(
    id           BIGSERIAL NOT NULL,
    order_number VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE t_order_line_items
(

    id        BIGSERIAL NOT NULL,
    order_id  BIGINT  NOT NULL,
    sku_code  VARCHAR(255),
    price     DECIMAL(19, 2),
    quantity  INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_t_order_line_items_order
        FOREIGN KEY (order_id) REFERENCES t_orders(id)
);