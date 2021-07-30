#!/bin/bash
# Use the "force" parameter to skip prompts
# Note for yada developers: the ${"\$"} in the yada template is the escaped form of the dollar sign

hostname=localhost
force=

if [ "$1" == "force" ]
then
	force=-f
fi

echo 
echo Rigenerazione del DB ${acronym}db$env

mysqladmin ${"\$"}force --user=root --password=$dbpwd drop ${acronym}db$env

# devo usare mysql e non mysqladmin perche' il secondo non mi setta il charset

mysql -u root --password=$dbpwd --host=${"\$"}hostname <<SQLCOMMAND 
create database ${acronym}db$env character set utf8mb4;
CREATE USER '${acronym}user$env'@'localhost' IDENTIFIED BY '$dbpwd';
GRANT ALL ON ${acronym}db$env.* TO '${acronym}user$env'@'localhost';
FLUSH PRIVILEGES;
SQLCOMMAND

mysql -u root --password=$dbpwd --host=${"\$"}hostname ${acronym}db$env < ${acronym}.sql
mysql -u root --password=$dbpwd --host=${"\$"}hostname ${acronym}db$env < ${acronym}extra.sql

echo Done.
