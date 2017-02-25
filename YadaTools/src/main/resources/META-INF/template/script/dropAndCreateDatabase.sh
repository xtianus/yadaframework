#!/bin/bash
# Usare il parametro "force" per bypassare il prompt
# NOTA: questo script VIENE COPIATO ogni volta dal build.xml del workspace, quindi non fare modifiche su ubuntu
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

mysql --verbose -u root --password=$dbpwd --host=${"\$"}hostname <<SQLCOMMAND 
create database ${acronym}db$env character set utf8;
CREATE USER '${acronym}user$env'@'localhost' IDENTIFIED BY '$dbpwd';
GRANT ALL ON ${acronym}db$env.* TO '${acronym}user$env'@'localhost';
FLUSH PRIVILEGES;
SQLCOMMAND

mysql --verbose -u root --password=$dbpwd --host=${"\$"}hostname ${acronym}db$env < ${acronym}.sql
mysql --verbose -u root --password=$dbpwd --host=${"\$"}hostname ${acronym}db$env < ${acronym}extra.sql

echo Done.
