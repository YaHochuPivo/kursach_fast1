-- Flyway migration: DB enhancements
-- 1) Fix CHECK constraint for properties.status to include 'archived'
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name='properties' AND constraint_name='properties_status_check'
    ) THEN
        EXECUTE 'ALTER TABLE properties DROP CONSTRAINT properties_status_check';
    END IF;
END $$;

ALTER TABLE properties
    ADD CONSTRAINT properties_status_check
    CHECK (status IN ('active','sold','hidden','archived'));

-- 2) DB-level audit log (generic minimal example for properties)
CREATE TABLE IF NOT EXISTS audit_log_db (
    id BIGSERIAL PRIMARY KEY,
    table_name TEXT NOT NULL,
    operation TEXT NOT NULL,
    at TIMESTAMP WITHOUT TIME ZONE DEFAULT now() NOT NULL,
    old_data JSONB,
    new_data JSONB
);

CREATE OR REPLACE FUNCTION fn_audit_properties()
RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO audit_log_db(table_name, operation, new_data)
        VALUES ('properties', 'INSERT', to_jsonb(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_log_db(table_name, operation, old_data, new_data)
        VALUES ('properties', 'UPDATE', to_jsonb(OLD), to_jsonb(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO audit_log_db(table_name, operation, old_data)
        VALUES ('properties', 'DELETE', to_jsonb(OLD));
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_properties ON properties;
CREATE TRIGGER trg_audit_properties
AFTER INSERT OR UPDATE OR DELETE ON properties
FOR EACH ROW EXECUTE FUNCTION fn_audit_properties();

-- 3) Reporting view example
CREATE OR REPLACE VIEW v_property_summary AS
SELECT 
    city,
    status,
    COUNT(*) AS cnt,
    COALESCE(ROUND(AVG(price)::numeric, 2), 0) AS avg_price
FROM properties
GROUP BY city, status;
