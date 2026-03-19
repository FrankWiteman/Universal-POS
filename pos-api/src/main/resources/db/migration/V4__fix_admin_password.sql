-- ============================================================
--  UniversalPOS — V4 Fix Admin Password Hash
--  The V2 seed data contained an incorrect BCrypt hash.
--  This migration corrects it with a verified hash.
--
--  Password: ChangeMe123!
--  Hash algorithm: BCrypt strength 12 ($2b$)
--  Spring Security BCryptPasswordEncoder accepts $2b$ hashes.
-- ============================================================

UPDATE EMPLOYEES
SET PASSWORD_HASH = '$2b$12$6LFU.WIohAjJUgFG2KpFte2dZ.Ap05ogzKq95vhOybpVTbbkU/ys6'
WHERE EMAIL = 'admin@universalpos.local'
AND TENANT_ID = (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store');

COMMIT;