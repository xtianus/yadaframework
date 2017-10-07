#!/bin/bash
# Database backup
# Keeps one month of history
# The dbconfig file must contain the username and password, like:
#   [mysqldump]
#   user=root
#   password=123123
# You should protect the file with chmod 600
# You can run it every six hours with
#   crontab -e
#   0 */6 * * * /somepath/backupdb.sh myschema /path/to/backupdb.cnf /path/to/backupdir

if (( $# < 3 )); then
	echo "Syntax: $( basename $0 ) <dbname> <dbconfig> <backupdir>"
	exit 1;
fi

dbname=$1
dbconfig=$2
backupdir=$3

mkdir -p $backupdir

mysqldump --defaults-file=$dbconfig $dbname | gzip > $backupdir/${dbname}_$( date +%d%H ).gz


