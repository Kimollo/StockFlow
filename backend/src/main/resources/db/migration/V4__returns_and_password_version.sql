ALTER TABLE sale_items
    ADD COLUMN returned_quantity INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_sale_items_returned_non_negative CHECK (returned_quantity >= 0),
    ADD CONSTRAINT ck_sale_items_returned_within_sold CHECK (returned_quantity <= quantity);

ALTER TABLE stock_movements
    DROP CONSTRAINT ck_stock_movements_sale_requires_sale,
    ADD CONSTRAINT ck_stock_movements_sale_requires_sale CHECK (
        movement_type NOT IN ('SALE', 'RETURN') OR sale_id IS NOT NULL
    );

ALTER TABLE users
    ADD COLUMN credentials_version INTEGER NOT NULL DEFAULT 0;
