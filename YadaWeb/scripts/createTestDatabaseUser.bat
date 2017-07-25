mysql.exe --verbose -u root -e "CREATE USER 'yadatest'@'localhost' IDENTIFIED BY 'yadatest'; GRANT ALL ON yadatestdb.* TO 'yadatest'@'localhost'; FLUSH PRIVILEGES;"

pause
