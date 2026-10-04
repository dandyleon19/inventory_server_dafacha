-- The same product can appear on several lines of an order: a package at one price, plus a few extra units that were
-- offered at another. Each line is received on its own and enters stock with its own cost.
ALTER TABLE purchase_order_lines
    DROP CONSTRAINT uq_po_line_product;
