-- Security tokens: autologin and registration/password recovery/email change tokens are stored as SHA-256 digests
-- (tokenHash) instead of plaintext numbers, and registration requests get an explicit expiration.
-- Outstanding links can't be converted, so every token row is deleted: users must request new links.
-- All rows are deleted before the first DDL statement because DDL is not transactional in MySQL and MariaDB.
-- YexRegistrationRequest (JOINED subclass) references YadaRegistrationRequest, so its rows go first.
-- An application with other YadaRegistrationRequest or YadaAutoLoginToken subclasses deletes their rows here too.
-- User credentials, roles and profiles are not touched.
DELETE FROM YexRegistrationRequest;
DELETE FROM YadaRegistrationRequest;
DELETE FROM YadaAutoLoginToken;

-- yadaUserCredentials is a ManyToOne: a user can hold several tokens and requests, so the unique constraints left by
-- the former OneToOne mapping are replaced by plain indexes for the foreign keys (as the current schema generation does).
ALTER TABLE YadaAutoLoginToken
	ADD INDEX FKh92vo7me2k2s4v1x1jercpuo (yadaUserCredentials_id),
	DROP INDEX UKgpwvvntka6p2tjtnut6qiokyi,
	DROP COLUMN token,
	ADD COLUMN tokenHash varchar(64) not null;

ALTER TABLE YadaRegistrationRequest
	ADD INDEX FKkn2yxfy3t9fjmuannqfph49d0 (trattamentoDati_id),
	ADD INDEX FKq6guqxscpqqq7pl96md1y79rn (yadaUserCredentials_id),
	DROP INDEX UK66pbcq6oohrfjqwr1o7wslucn,
	DROP INDEX UKb0i98ixarlwa54gkxws9ykxae,
	DROP COLUMN token,
	ADD COLUMN expiration TIMESTAMP NULL null,
	ADD COLUMN tokenHash varchar(64) not null;
