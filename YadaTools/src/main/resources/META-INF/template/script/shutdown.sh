#!/bin/bash
# Script that stops a server and waits until the process is no more active,
# with a timeout of 10 seconds

# Command to stop the process
stopCommand='sudo -u tomcat8 $basePath/$acronym$env/tomcat/bin/shutdown.sh'

# Identification string for the process
ident='/srv/$acronym$env/tomcat/bin/bootstrap.jar'

myPid=$( ps -ww -C java -o pid,args | grep $ident | awk '{print $1}' )

$stopCommand

if [ "$myPid" != "" ]; then

        COUNTER=0
        while [  $COUNTER -lt 10 ]; do
                process=$( ps -h $myPid )
                if [ "$process" = "" ]; then
                        COUNTER=99;
                else
                        echo Waiting for process to stop...
                        sleep 1
                        let COUNTER=COUNTER+1
                fi
        done

        if [ "$COUNTER" != "99" ]; then
                echo "!!!! Can't stop server !!!!"
                exit 1
        fi
fi



