INSERT INTO product (id, name, unit_price) VALUES
    ('P-001', 'Ground coffee 250 g', 3.99),
    ('P-002', 'Semi-skimmed milk 1 L', 1.09),
    ('P-003', 'Traditional baguette', 2.50);

INSERT INTO discount_code (code, percentage, active, starts_at, expires_at) VALUES
    ('SUMMER10', 10, TRUE, NULL, NULL),
    ('WELCOME5', 5, TRUE, NULL, NULL),
    ('EXPIRED20', 20, TRUE, '2024-01-01T00:00:00Z', '2025-01-01T00:00:00Z'),
    ('FUTURE25', 25, TRUE, '2099-01-01T00:00:00Z', NULL),
    ('DISABLED15', 15, FALSE, NULL, NULL);
