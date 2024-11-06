#!/bin/bash
# Use the "force" parameter to skip prompts
# Note for yada developers: the $ in the yada template is the escaped form of the dollar sign

hostname=localhost
force=

if [ "$1" == "force" ]
then
	force=-f
fi

echo 
echo Rigenerazione del DB yexdbprod

mysqladmin $force --user=root --password=qwe drop yexdbprod

# devo usare mysql e non mysqladmin perche' il secondo non mi setta il charset

mysql -u root --password=qwe --host=$hostname <<SQLCOMMAND 
create database yexdbprod character set utf8mb4;
CREATE USER 'yexuserprod'@'localhost' IDENTIFIED BY 'qwe';
GRANT ALL ON yexdbprod.* TO 'yexuserprod'@'localhost';
FLUSH PRIVILEGES;
SQLCOMMAND

mysql -u root --password=qwe --host=$hostname yexdbprod < yex.sql
mysql -u root --password=qwe --host=$hostname yexdbprod < yexextra.sql

echo Done.
