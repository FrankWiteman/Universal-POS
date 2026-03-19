-- Extra grants for the universalpos app user
-- gvenzl/oracle-xe creates the APP_USER automatically;
-- this file runs inside that user's schema on first boot.

ALTER SESSION SET CURRENT_SCHEMA = universalpos;

-- Ensure quota on the default tablespace
ALTER USER universalpos QUOTA UNLIMITED ON USERS;
