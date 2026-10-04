-- How a purchase order line was bought, as the user described it ("1 paquete de 12 + 6 sueltas, total S/ 45"). Only
-- for reading: the quantity and the unit cost of the line (in the product's own unit) are what the system uses.
ALTER TABLE purchase_order_lines
    ADD COLUMN packaging VARCHAR(200);
