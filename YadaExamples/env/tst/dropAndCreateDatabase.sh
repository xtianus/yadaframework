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
echo Rigenerazione del DB yexdbtst

mysqladmin $force --user=root --password=qwe drop yexdbtst

# devo usare mysql e non mysqladmin perche' il secondo non mi setta il charset

mysql -u root --password=qwe --host=$hostname <<SQLCOMMAND 
create database yexdbtst character set utf8mb4;
CREATE USER 'yexusertst'@'localhost' IDENTIFIED BY 'qwe';
GRANT ALL ON yexdbtst.* TO 'yexusertst'@'localhost';
FLUSH PRIVILEGES;
SQLCOMMAND

mysql -u root --password=qwe --host=$hostname yexdbtst < yex.sql
mysql -u root --password=qwe --host=$hostname yexdbtst < yexextra.sql

echo Done.
