-- Flyway migration V2: procedures, generic audit triggers, extra views

-- 0) Helper: admin check by email
CREATE OR REPLACE FUNCTION fn_is_admin(p_email text)
RETURNS boolean AS $$
DECLARE v_role text;
BEGIN
  SELECT role::text INTO v_role FROM users WHERE email = p_email LIMIT 1;
  RETURN coalesce(v_role = 'ADMIN', false);
END; $$ LANGUAGE plpgsql;

-- 1) Helper: whether user can edit property (owner or admin)
CREATE OR REPLACE FUNCTION fn_can_edit_property(p_id bigint, p_email text)
RETURNS boolean AS $$
DECLARE v_owner_id bigint; v_user_id bigint; v_admin boolean;
BEGIN
  SELECT id INTO v_user_id FROM users WHERE email = p_email LIMIT 1;
  IF v_user_id IS NULL THEN RETURN false; END IF;
  v_admin := fn_is_admin(p_email);
  IF v_admin THEN RETURN true; END IF;
  SELECT user_id INTO v_owner_id FROM properties WHERE id = p_id;
  RETURN coalesce(v_owner_id = v_user_id, false);
END; $$ LANGUAGE plpgsql;

-- 2) Stored procedures: archive / unarchive property
CREATE OR REPLACE PROCEDURE sp_archive_property(p_id bigint, p_email text)
LANGUAGE plpgsql AS $$
BEGIN
  IF NOT fn_can_edit_property(p_id, p_email) THEN
    RAISE EXCEPTION 'forbidden';
  END IF;
  UPDATE properties SET status = 'archived' WHERE id = p_id;
END;$$;

CREATE OR REPLACE PROCEDURE sp_unarchive_property(p_id bigint, p_email text)
LANGUAGE plpgsql AS $$
DECLARE v_status text;
BEGIN
  IF NOT fn_can_edit_property(p_id, p_email) THEN
    RAISE EXCEPTION 'forbidden';
  END IF;
  SELECT status::text INTO v_status FROM properties WHERE id = p_id;
  IF v_status = 'archived' THEN
    UPDATE properties SET status = 'active' WHERE id = p_id;
  END IF;
END;$$;

-- 3) Stored procedure: unpromote all properties of a user
CREATE OR REPLACE PROCEDURE sp_unpromote_all_by_email(p_email text)
LANGUAGE plpgsql AS $$
DECLARE v_user_id bigint;
BEGIN
  SELECT id INTO v_user_id FROM users WHERE email = p_email LIMIT 1;
  IF v_user_id IS NULL THEN RAISE EXCEPTION 'user_not_found'; END IF;
  UPDATE properties SET promoted = false WHERE user_id = v_user_id AND coalesce(promoted, false) = true;
END;$$;

-- 4) Generic audit trigger function (replaces table-specific one)
CREATE OR REPLACE FUNCTION fn_audit_generic()
RETURNS trigger AS $$
DECLARE tname text := TG_TABLE_NAME; op text := TG_OP; 
BEGIN
  IF TG_OP = 'INSERT' THEN
    INSERT INTO audit_log_db(table_name, operation, new_data) VALUES (tname, op, to_jsonb(NEW));
    RETURN NEW;
  ELSIF TG_OP = 'UPDATE' THEN
    INSERT INTO audit_log_db(table_name, operation, old_data, new_data) VALUES (tname, op, to_jsonb(OLD), to_jsonb(NEW));
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    INSERT INTO audit_log_db(table_name, operation, old_data) VALUES (tname, op, to_jsonb(OLD));
    RETURN OLD;
  END IF;
  RETURN NULL;
END; $$ LANGUAGE plpgsql;

-- Rewire properties to generic trigger
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_audit_properties') THEN
    EXECUTE 'DROP TRIGGER trg_audit_properties ON properties';
  END IF;
  EXECUTE 'CREATE TRIGGER trg_audit_properties AFTER INSERT OR UPDATE OR DELETE ON properties FOR EACH ROW EXECUTE FUNCTION fn_audit_generic()';
END $$;

-- Optionally attach audit trigger to deals table if it exists
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename='deals') THEN
    IF EXISTS (SELECT 1 FROM pg_trigger t JOIN pg_class c ON t.tgrelid=c.oid WHERE t.tgname='trg_audit_deals' AND c.relname='deals') THEN
      EXECUTE 'DROP TRIGGER trg_audit_deals ON deals';
    END IF;
    EXECUTE 'CREATE TRIGGER trg_audit_deals AFTER INSERT OR UPDATE OR DELETE ON deals FOR EACH ROW EXECUTE FUNCTION fn_audit_generic()';
  END IF;
END $$;

-- 5) View: deal summary per status and city if deals table exists
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename='deals') THEN
    EXECUTE 'CREATE OR REPLACE VIEW v_deal_summary AS \n'
         || 'SELECT coalesce(city, ''—'') AS city, status, COUNT(*) AS cnt, \n'
         || '       COALESCE(ROUND(AVG(price)::numeric, 2), 0) AS avg_price \n'
         || 'FROM deals GROUP BY coalesce(city, ''—''), status';
  END IF;
END $$;
