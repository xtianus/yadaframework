@echo off
echo Rigenerazione del DB
echo (mysql.exe deve essere nel PATH di sistema)

mysqladmin --user=root --host=localhost drop ${acronym}db$env
mysqladmin --user=root --host=localhost refresh

REM devo usare mysql e non mysqladmin perche' il secondo non mi setta il charset

mysql.exe --verbose -u root -h localhost -e "create database ${acronym}db$env character set utf8mb4;"

mysql.exe --verbose -u root -h localhost ${acronym}db$env < $schemaFolderPath${"\\"}${acronym}.sql
REM Uncomment the following if some custom sql is needed to create the tables
REM mysql.exe --verbose -u root -h localhost ${acronym}db$env < $schemaFolderPath${"\\"}${acronym}extra.sql

echo Done.
 
 
