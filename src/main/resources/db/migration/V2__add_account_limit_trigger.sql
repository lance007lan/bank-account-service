CREATE OR REPLACE FUNCTION check_account_limit()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT COUNT(*) FROM accounts WHERE customer_name = NEW.customer_name) >= 5 THEN
        RAISE EXCEPTION 'Customer already has the maximum of 5 accounts';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--- Validate if we have 5 accounts already before inserting ------
CREATE TRIGGER enforce_account_limit
    BEFORE INSERT ON accounts
    FOR EACH ROW EXECUTE FUNCTION check_account_limit();
