mysql.exe --verbose -u root -h localhost -e "create database yexdbdev character set utf8mb4;"
mysql.exe --verbose -u root -h localhost -e "CREATE USER 'yexuserdev'@'localhost' IDENTIFIED BY 'qwe'; GRANT ALL ON yexdbdev.* TO 'yexuserdev'@'localhost'; FLUSH PRIVILEGES;"

pause
