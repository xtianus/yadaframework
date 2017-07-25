
mysqladmin --force --user=root drop yadatestdb
mysqladmin --user=root refresh
mysql.exe --verbose -u root -e "create database yadatestdb character set utf8mb4;"
mysql.exe --verbose -u root yadatestdb < schema\yadatest.sql

pause