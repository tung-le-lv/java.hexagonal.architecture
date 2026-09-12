-- Order service schema.
--
-- Written as plain SQL under Flyway rather than generated from the JPA entities: the schema is a
-- deliberate artefact with its own lifecycle, and `ddl-auto` would make the database a side effect of
-- whatever the mapping happens to look like today. Types are chosen to be valid on both PostgreSQL
-- (production) and H2 (tests), and TIMESTAMP WITH TIME ZONE matches how Hibernate 6 stores Instant.

CREATE TABLE orders (
    id                  UUID                      NOT NULL,
    customer_id         UUID                      NOT NULL,
    status              VARCHAR(32)               NOT NULL,
    currency            VARCHAR(3)                NOT NULL,
    street              VARCHAR(255)              NOT NULL,
    city                VARCHAR(128)              NOT NULL,
    postal_code         VARCHAR(32)               NOT NULL,
    country_code        VARCHAR(2)                NOT NULL,
    discount_amount     NUMERIC(19, 4)            NOT NULL,
    -- Denormalised from the lines so the list query needs no join; the aggregate remains the
    -- authority and this column is written from it on every save.
    total_amount        NUMERIC(19, 4)            NOT NULL,
    created_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    placed_at           TIMESTAMP(6) WITH TIME ZONE,
    paid_at             TIMESTAMP(6) WITH TIME ZONE,
    shipped_at          TIMESTAMP(6) WITH TIME ZONE,
    cancelled_at        TIMESTAMP(6) WITH TIME ZONE,
    cancellation_reason VARCHAR(512),
    payment_reference   VARCHAR(128),
    tracking_number     VARCHAR(128),
    version             BIGINT                    NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_lines (
    id           UUID           NOT NULL,
    order_id     UUID           NOT NULL,
    product_id   UUID           NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    unit_price   NUMERIC(19, 4) NOT NULL,
    quantity     INTEGER        NOT NULL,
    CONSTRAINT pk_order_lines PRIMARY KEY (id),
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_lines_unit_price CHECK (unit_price >= 0),
    -- The aggregate merges a repeated product into one line; this makes the database agree, so the
    -- invariant survives any write that does not go through the model.
    CONSTRAINT uq_order_lines_order_product UNIQUE (order_id, product_id)
);

CREATE INDEX idx_order_lines_order ON order_lines (order_id);

-- Transactional outbox: written in the same transaction as the order change it describes, then
-- relayed to the broker asynchronously. This is what makes "saved but never published" impossible.
CREATE TABLE outbox_messages (
    id             UUID                        NOT NULL,
    aggregate_type VARCHAR(64)                 NOT NULL,
    aggregate_id   VARCHAR(64)                 NOT NULL,
    event_type     VARCHAR(128)                NOT NULL,
    payload        VARCHAR(8192)               NOT NULL,
    occurred_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    published_at   TIMESTAMP(6) WITH TIME ZONE,
    attempts       INTEGER                     NOT NULL DEFAULT 0,
    last_error     VARCHAR(1024),
    CONSTRAINT pk_outbox_messages PRIMARY KEY (id)
);

-- Supports the relay's only query: unpublished messages, oldest first.
CREATE INDEX idx_outbox_unpublished ON outbox_messages (published_at, occurred_at);
CREATE INDEX idx_outbox_aggregate ON outbox_messages (aggregate_type, aggregate_id);
