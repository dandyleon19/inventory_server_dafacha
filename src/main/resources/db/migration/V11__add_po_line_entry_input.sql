-- What the user typed to describe a purchase order line (packages, loose units, how the price was given), kept as the
-- form wrote it so that editing the draft shows the line exactly as it was entered. The server doesn't interpret
-- it: quantity and unit_cost (in the product's own unit) are what counts.
ALTER TABLE purchase_order_lines
    ADD COLUMN entry_input VARCHAR(1000);
