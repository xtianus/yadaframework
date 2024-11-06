@echo off
echo Database regeneration: all data will be lost!
echo (mysql.exe must be in the system PATH)

mysqladmin --user=root --host=localhost drop yexdbdev
mysqladmin --user=root --host=localhost refresh

mysql.exe -u root -h localhost -e "create database yexdbdev character set utf8mb4;"

mysql.exe -u root -h localhost yexdbdev < C:\work\gits\YadaDevelopment\yadaframework\YadaExamples\schema\yex.sql
REM Uncomment the following if some custom sql is needed to create the tables
REM mysql.exe -u root -h localhost yexdbdev < C:\work\gits\YadaDevelopment\yadaframework\YadaExamples\schema\yexextra.sql

echo Done.
 
 
