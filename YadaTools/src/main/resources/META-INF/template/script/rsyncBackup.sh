#!/bin/bash
# Copy a folder to a remote server via rsync
# Should be set in cron daily:
# ln -s /myPath/rsyncBackup.sh /etc/cron.daily/rsyncbackup

targetIP=xxx.xxx.xxx.xxx
user=<targetUser>
targetPath=backup/myproject
options='--delete -avv'
logfile=/var/log/rsync_client.log
export RSYNC_PASSWORD='myPassword'

# Trailing slash is important to skip folder creation on destination
srcDir=/home/<somePath>/backup/

# Choose a type of rsync connection
# This works if you open the rsync port on the target
# nice -n 20 rsync $options $srcDir ${user}@${targetIP}::${targetPath} >> $logfile
# This works via ssh
# The -i <pathToPrivateKey> option is not needed if servers are configured for mutual ssh authentication
# nice -n 20 rsync -e "ssh -i <pathToPrivateKey>" $options $srcDir ${user}@${targetIP}:${targetPath} >> $logfile
# Delete the following line when the rsync connection has been chosen
echo Error: you need to choose the rsync connection

