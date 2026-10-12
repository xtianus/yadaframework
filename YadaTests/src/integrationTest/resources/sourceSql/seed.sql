USE `@SCHEMA@`;
CREATE TABLE seeded_item (id INT PRIMARY KEY, label VARCHAR(32) NOT NULL);
INSERT INTO seeded_item VALUES (1, 'first'), (2, 'second');
