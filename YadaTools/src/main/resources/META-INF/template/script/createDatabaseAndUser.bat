mysql.exe --verbose -u root -h localhost -e "create database ${acronym}db$env character set utf8;"
mysql.exe --verbose -u root -h localhost -e "CREATE USER '${acronym}user$env'@'localhost' IDENTIFIED BY '$dbpwd'; GRANT ALL ON ${acronym}db$env.* TO '${acronym}user$env'@'localhost'; FLUSH PRIVILEGES;"

pause
