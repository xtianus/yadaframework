#!/bin/bash

sudo -u tomcat8 touch $basePath/$acronym$env/logs/restarted
sudo -u tomcat8 $basePath/$acronym$env/tomcat/bin/startup.sh

rm $basePath/$acronym$env/bin/watchdog.paused 2>/dev/null