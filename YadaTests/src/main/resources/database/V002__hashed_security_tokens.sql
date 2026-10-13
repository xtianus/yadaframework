-- Security tokens: autologin and registration/password recovery/email change tokens are stored as SHA-256 digests
-- (tokenHash) instead of plaintext numbers, and registration requests get an explicit expiration.
-- Outstanding links can't be converted, so every token row is deleted: users must request new links.
-- The rows are deleted before the DDL because DDL statements are not transactional in MariaDB and MySQL.
-- No table references these rows (YadaTests has no YadaRegistrationRequest subclass); user accounts are not touched.
DELETE FROM YadaAutoLoginToken;
DELETE FROM YadaRegistrationRequest;

ALTER TABLE YadaAutoLoginToken
	DROP COLUMN token,
	ADD COLUMN tokenHash varchar(64) not null;

ALTER TABLE YadaRegistrationRequest
	DROP COLUMN token,
	ADD COLUMN expiration TIMESTAMP NULL null,
	ADD COLUMN tokenHash varchar(64) not null;
