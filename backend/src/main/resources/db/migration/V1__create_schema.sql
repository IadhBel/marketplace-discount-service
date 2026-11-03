CREATE TABLE product (
    id         VARCHAR(50)    PRIMARY KEY,
    name       VARCHAR(255)   NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL CHECK (unit_price > 0)
);

CREATE TABLE discount_code (
    code       VARCHAR(50) PRIMARY KEY CHECK (code = UPPER(code)),
    percentage INTEGER     NOT NULL CHECK (percentage BETWEEN 1 AND 100),
    active     BOOLEAN     NOT NULL,
    starts_at  TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    CHECK (starts_at IS NULL OR expires_at IS NULL OR starts_at < expires_at)
);
