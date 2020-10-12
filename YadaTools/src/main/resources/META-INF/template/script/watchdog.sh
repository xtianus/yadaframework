#!/bin/bash
# Controlla che un processo sia attivo e lo rilancia quando non lo trova.
# Non esegue il controllo se trova un file di semaforo sul filesystem (utile per i riavvii normali).
# DA METTERE IN CRON:
# * * * * * $basePath/$acronym$env/bin/watchdog.sh
# Note for yada developers: the ${"\$"} in the yada template is the escaped form of the dollar sign

baseDir='$basePath/$acronym$env'
startCommand="${"\$"}baseDir/bin/site-startup.sh"
# Identification string for the process
ident="${"\$"}baseDir/tomcat/bin/bootstrap.jar"
semaphoreFile=${"\$"}baseDir/bin/watchdog.paused
logFile=${"\$"}baseDir/logs/watchdog.log

myPid=${"\$"}( ps -ww -C java -o pid,args | grep ${"\$"}ident | awk '{print ${"\$"}1}' )
if [ "${"\$"}myPid" = "" ]; then
	echo "${"\$"}( date ): **** watchdog non trova il processo! ****" >> ${"\$"}logFile
	if [ ! -e "${"\$"}semaphoreFile" ]; then
		echo "${"\$"}( date ): watchdog rilancia il sito" >> ${"\$"}logFile
		${"\$"}startCommand &>> ${"\$"}logFile
	else
		echo "${"\$"}( date ): watchdog in pausa su ${"\$"}semaphoreFile" >> ${"\$"}logFile
	fi
fi

