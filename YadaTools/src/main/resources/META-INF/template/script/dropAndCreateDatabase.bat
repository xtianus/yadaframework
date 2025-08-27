@echo off
echo Database regeneration: all data will be lost!
echo (mysql.exe must be in the system PATH)

mysqladmin --user=root --host=localhost drop ${acronym}db$env
mysqladmin --user=root --host=localhost refresh

mysql.exe -u root -h localhost -e "create database ${acronym}db$env character set utf8mb4;"

mysql.exe -u root -h localhost ${acronym}db$env < $schemaFolderPath${"\\"}${acronym}.sql
REM Uncomment the following if some custom sql is needed to create the tables
REM mysql.exe -u root -h localhost ${acronym}db$env < $schemaFolderPath${"\\"}${acronym}extra.sql

echo Done.
 
 
