mysql.exe --verbose -u root -h localhost -e "create database ${acronym}db$env character set utf8mb4;"
mysql.exe --verbose -u root -h localhost -e "CREATE USER '${acronym}user$env'@'%' IDENTIFIED BY '$dbpwd'; GRANT ALL ON ${acronym}db$env.* TO '${acronym}user$env'@'%'; FLUSH PRIVILEGES;"

pause
