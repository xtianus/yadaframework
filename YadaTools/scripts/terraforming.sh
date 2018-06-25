#!/bin/bash
# Initial setup of a bare new server.
# The current user is root.
# 26 April 2018
#
# TODO: many tomcat instances (copy to different numbered folders, change ports, configure mod-jk for load balancing)
# TODO: mysql root password
#
# Parameters: <hostname> <virtualHost> <projectBasePath> [<myip>] [<deployOptions>]

if (( $# < 3 )); then
	echo "Syntax: $( basename $0 ) <hostname> <virtualHost> <projectBasePath> [<myip>] [<deployOptions>]"
	exit 1;
fi

myHostName=$1
# e.g. myproject.com
myVirtualHost=$2
# projectBase e.g. /srv/ldbprod
projectBase=$3
MYIP=$4
# Configuration override
deployOptions=$5

# e.g. ldbprod
projectName=${projectBase##*/}

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

if [[ ! $MYIP ]]; then
	MYIP=$( ifconfig eth0 | awk '/inet addr/{print substr($2,6)}' )
fi
if [[ ! $MYIP ]]; then
	MYIP=$( ifconfig venet0:0 | awk '/inet addr/{print substr($2,6)}' )
fi

if [[ ! $MYIP ]]; then
   echo ERROR: Failed to retrieve IP address
   exit 1 
fi

if [ -f $DIR/terraforming.cfg ]; then
	source $DIR/terraforming.cfg
else
    echo ERROR: Failed to load terraforming.cfg
    exit 1 
fi

if [[ $deployOptions ]]; then
	eval $deployOptions
fi

viconfig=/etc/vim/vimrc
homedir=/home/${cfgUser}

if [ ! -d ${projectBase} ]; then
	mkdir -p ${projectBase}/logs
	mkdir ${projectBase}/bin
	mkdir ${projectBase}/contents
	mkdir ${projectBase}/deploy
fi

if [[ $cfgHostname && $myHostName ]]; then
	hostname $myHostName
	hostname > /etc/hostname
fi

apt-get update

export DEBIAN_FRONTEND=noninteractive DEBCONF_NONINTERACTIVE_SEEN=true
 
apt-get -o Dpkg::Options::="--force-confnew" -y upgrade

echo -e "\n" >> $viconfig
echo set background=dark >> $viconfig
echo set showcmd >> $viconfig
echo set showmatch >> $viconfig
echo set ignorecase >> $viconfig
echo set smartcase >> $viconfig
echo set incsearch >> $viconfig

apt-get -o Dpkg::Options::="--force-confnew" install sudo cron
echo "${cfgUser}	ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers;

groupadd ${cfgUser}

# The \"'$cfgUserPwd'\" syntax is needed to cope with special characters in the password
# See https://askubuntu.com/questions/644092/bash-script-and-escaping-special-characters-in-password
useradd ${cfgUser} -p \"'$cfgUserPwd'\" -m -g ${cfgUser} -G root -s /bin/bash

echo -e "\nAllowUsers ${cfgUser}\nPasswordAuthentication no\n" >> /etc/ssh/sshd_config

mkdir ${homedir}/.ssh
echo $cfgAuthorizedKeys > ${homedir}/.ssh/authorized_keys2

chown -R ${cfgUser}:${cfgUser} ${homedir}/.ssh
chmod 700 ${homedir}/.ssh
chmod 600 ${homedir}/.ssh/authorized_keys2

if [ "$cfgNoUfw" != "true" ]; then
	apt-get -o Dpkg::Options::="--force-confnew" -y install ufw
	ufw allow 22
	ufw allow 443
	ufw allow 80
	if [ "$cfgEmail" != "false" ]; then
		ufw allow 25
	fi;
	ufw limit ssh/tcp
	ufw status
	ufw --force enable
fi;

service ssh restart

apt-get -o Dpkg::Options::="--force-confnew" -y install $cfgPkgJava
if [[ $cfgPkgTomcat && ! $cfgTomcatTarGz ]]; then
	apt-get -o Dpkg::Options::="--force-confnew" -y install $cfgPkgTomcat $cfgPkgTomcatUtil
fi

if [[ $cfgTomcatTarGz ]]; then
	# Per il link all'ultima versione andare su http://tomcat.apache.org/ e copiare il link dalla pagina di download
	mkdir -p /srv/tomcats
	cd /srv/tomcats
	wget $cfgTomcatTarGz
	tomcatFilename=${cfgTomcatTarGz##*/}
	prefix=${tomcatFilename%.tar.gz}
	mv $tomcatFilename $prefix.cloneable.tar.gz
	tar xvzf $prefix.cloneable.tar.gz
	if [ "$cfgTomcatUser" != "root" ]; then
		adduser --system --no-create-home $cfgTomcatUser
		groupadd --system $cfgTomcatUser
		usermod -g $cfgTomcatUser $cfgTomcatUser
	fi
	mv $prefix $cfgTomcatTarGzHome
	chown -R root:$cfgTomcatUser $cfgTomcatTarGzHome
	chmod -R g+r $cfgTomcatTarGzHome
	chmod -R g+w $cfgTomcatTarGzHome/work
	chmod -R g+w $cfgTomcatTarGzHome/logs
	chmod -R g+w $cfgTomcatTarGzHome/temp
	chmod g+wx $cfgTomcatTarGzHome/conf
fi
#
if [[ $cfgTomcatNativeTarGz ]]; then
	mkdir -p /srv/tomcats/native
	cd /srv/tomcats/native
	destination=/usr
	apt install gcc make libapr1-dev libssl-dev
	wget $cfgTomcatNativeTarGz
	nativeFilename=${cfgTomcatNativeTarGz##*/}
	prefix=${nativeFilename%.tar.gz}
	tar xvzf $nativeFilename
	cd $prefix/native
	javacPath=$( readlink -f `which javac` )
	javaHome=${javacPath%/bin/javac}
	./configure --prefix=$destination --with-java-home=$javaHome --with-apr=`which apr-1-config` --with-ssl=yes
	make
	make install
fi

# Options for java 9
# tomcatOptions="-Xmx${cfgTomcatRam} -Djava.awt.headless=true -Xlog:gc*:file=${projectBase}/logs/tomcat-gc.log:time:filecount=4,filesize=8192 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${projectBase}/logs/tomcat-outofmemory-dump.hprof"
# Options for java 8
tomcatOptions="-Xloggc:${projectBase}/logs/tomcat-gc.log -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=1 -XX:GCLogFileSize=1M -XX:+PrintGCDateStamps -Djava.awt.headless=true -Xmx${cfgTomcatRam} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${projectBase}/logs/tomcat-outofmemory-dump.hprof"

# Tomcat configuration
if [[ $cfgPkgTomcat ]]; then
	CATALINA_HOME=/usr/share/$cfgPkgTomcat
	CATALINA_BASE=/var/lib/$cfgPkgTomcat
	tomcatConfiguration=/etc/default/$cfgPkgTomcat
	# Non uso -XX:+UseConcMarkSweepGC perché le vm economiche non hanno tante cpu  
	sed -i 's%^JAVA_OPTS=.*%JAVA_OPTS=$tomcatOptions%g' ${tomcatConfiguration}
	# Enable default remote debugger
	if [[ $cfgPkgTomcatDebugger ]]; then
		sed -i 's%^#JAVA_OPTS="${JAVA_OPTS} -Xdebug%JAVA_OPTS="${JAVA_OPTS} -Xdebug%' ${tomcatConfiguration}
	fi
	if [[ $cfgTomcatManagerPwd ]]; then
		# Utenza tomcat manager
		sed -i 's%</tomcat-users>%<role rolename="manager-gui"/><role rolename="manager-jmx"/><role rolename="admin"/><user username="${cfgUser}" password="${cfgTomcatManagerPwd}" roles="admin,manager-gui,manager-jmx"/></tomcat-users>%g' ${cfgTomcatBase}/tomcat-users.xml
	fi
	# Compression e timeout
	sed -i 's/connectionTimeout="20000"/connectionTimeout="'${cfgTomcatTimeout}'"\ncompression="on" compressableMimeType="text\/html,text\/xml,text\/plain,text\/css,application\/xml,text\/javascript,application\/javascript,application\/x-javascript,application\/pdf,application\/json,text\/json"/g' ${CATALINA_BASE}/conf/server.xml
	systemctl daemon-reload
fi
if [[ $cfgTomcatTarGzHome ]]; then
	echo "CATALINA_OPTS=\"$tomcatOptions\"" > $cfgTomcatTarGzHome/bin/setenv.sh
	# Compression e timeout
	sed -i 's/connectionTimeout="20000"/connectionTimeout="'${cfgTomcatTimeout}'"\ncompression="on" compressableMimeType="text\/html,text\/xml,text\/plain,text\/css,application\/xml,text\/javascript,application\/javascript,application\/x-javascript,application\/pdf,application\/json,text\/json"/g' $cfgTomcatTarGzHome/conf/server.xml
fi

# MySQL + Apache + ModJK + php
if [[ ! $cfgPkgApache ]]; then
	service apache2 stop
	dpkg --purge apache2-mpm-prefork apache2
fi
if [[ $cfgPkgApache ]]; then
	apt-get -o Dpkg::Options::="--force-confnew" -y install $cfgPkgApache
	a2enmod rewrite
	
	if [[ $cfgPkgModJk ]]; then
		apt-get -o Dpkg::Options::="--force-confnew" -y install $cfgPkgModJk
		sed -i 's/JkLogLevel info/JkLogLevel warn/g' /etc/apache2/mods-available/jk.conf
		a2enmod jk
		# Enabling AJP Connector
		sed -i 's%\(<Connector port="8009".*\)%-->\n&\n<!--%g' ${CATALINA_BASE}/conf/server.xml
	fi
	if [[ $myHostName ]]; then
		echo "ServerName $( hostname )" > /etc/apache2/conf-available/servername.conf
		a2enconf servername
	fi
	
	# Courtesy page
	if [ -d defaultCourtesyPage ]; then
		mv defaultCourtesyPage ${projectBase}/contents/pleaseWait
		echo > /etc/apache2/sites-available/pleaseWait.$myVirtualHost.conf "<VirtualHost *:80>
		    ServerName $myVirtualHost
		    ServerAdmin admin@$myVirtualHost
		
		    DocumentRoot $projectBase/contents
		    Alias /contents $projectBase/contents
		    <Directory $projectBase/contents>
		            Options FollowSymLinks
		            AllowOverride None
		            Require all granted
		    </Directory>
		
		    RewriteEngine on
		    RewriteCond %{REQUEST_URI} !^/contents
		    RewriteRule .* /contents/pleaseWait/pleaseWait.html [R=302,L]
		
		    ErrorLog \${APACHE_LOG_DIR}/${projectName}_error.log
		    LogLevel warn
		    CustomLog \${APACHE_LOG_DIR}/${projectName}_access.log combined
		</VirtualHost>"
		
	fi
	WORKER=${projectName}_worker
	echo > /etc/apache2/sites-available/$myVirtualHost.conf "<VirtualHost *:80>
        ServerName $myVirtualHost
        ServerAlias *.$myVirtualHost
        ServerAdmin admin@$myVirtualHost

        DocumentRoot $projectBase/contents
        Alias /contents $projectBase/contents
        <Directory $projectBase/contents>
                Options FollowSymLinks
                AllowOverride None
                Require all granted
        </Directory>

        ErrorLog \${APACHE_LOG_DIR}/${projectName}_error.log
        LogLevel warn
        CustomLog \${APACHE_LOG_DIR}/${projectName}_access.log combined

        JkMount /* $WORKER
        JkUnMount /contents/* $WORKER
        # For certbot
        JkUnMount /.well-known/* $WORKER
        
	</VirtualHost>"
	a2ensite $myVirtualHost
	
	# ModJK Worker
	modjkFile=/etc/libapache2-mod-jk/workers.properties
	alreadyThere=$( grep -Fc "$WORKER" $modjkFile )
	if (( $alreadyThere == 0 )); then
	        sed -i "s/\(worker.list=.*\)$/\1,$WORKER/g" $modjkFile
	        sed -i "\$aworker.$WORKER.port=8009" $modjkFile
	        sed -i "\$aworker.$WORKER.host=localhost" $modjkFile
	        sed -i "\$aworker.$WORKER.type=ajp13" $modjkFile
	        echo "Worker $WORKER added to $modjkFile. Apache restart needed!"
	else
	        echo "Worker $WORKER already configured in $modjkFile (skipped)"
	fi
	
	sed -i 's/JkLogLevel info/JkLogLevel warn/g' /etc/apache2/mods-available/jk.conf
	
	a2enmod jk
	a2dissite 000-default
	service apache2 restart
fi
if [[ $cfgPkgPhp ]]; then
	apt-get -o Dpkg::Options::="--force-confnew" -y install $cfgPkgPhp
	# Questo velocizza il php
	apt-get -o Dpkg::Options::="--force-confnew" -y install php-apc
fi
if [[ $cfgPkgMysql ]]; then
	apt-get -o Dpkg::Options::="--force-confnew" -y install $cfgPkgMysql
	# TODO DOES NOT WORK ANYMORE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	# mysqladmin -u root password ${cfgMysqlRootPwd}
	if [[ $cfgPkgMysqlConf ]]; then
		sed -i 's#\(\[mysql\].*\)#&\ndefault-character-set = utf8mb4#g' $cfgPkgMysqlConf
	fi
	if [[ $cfgPkgMysqldConf ]]; then
		sed -i 's#\(\[mysqld\].*\)#&\ncharacter-set-client-handshake = FALSE\ncharacter-set-server = utf8mb4\ncollation-server = utf8mb4_unicode_ci\n#g' $cfgPkgMysqldConf
	fi
fi

# certbot for SSL
if [[ $cfgCertbot != "false" ]]; then
	apt-get -o Dpkg::Options::="--force-confnew" -y install software-properties-common
	add-apt-repository -y ppa:certbot/certbot
	apt-get -o Dpkg::Options::="--force-confnew" -y update
	if [[ $cfgPkgApache ]]; then
		apt-get -o Dpkg::Options::="--force-confnew" -y install python-certbot-apache
	else
		apt-get -o Dpkg::Options::="--force-confnew" -y install certbot
	fi 
fi

# Altro
# Spiegato qui:  http://stackoverflow.com/questions/8671308/non-interactive-method-for-dpkg-reconfigure
echo -e "\ntzdata tzdata/Areas select Europe\ntzdata tzdata/Zones/Europe select Rome\n" > /tmp/preseed.txt
debconf-set-selections /tmp/preseed.txt
# Siccome non son sicuro che funzioni, faccio anche questo:
echo "Europe/Rome" > /etc/timezone
#
dpkg-reconfigure -f noninteractive tzdata

# Stampa l'hostname ad ogni login
sed -i '$aecho\necho "***********"\necho "* '$( hostname )' *"\necho "***********"\necho'  /etc/bash.bashrc

# su - ${cfgUser}
# Cambiare il colore di ls: directory gialle
sudo -u ${cfgUser} dircolors -p > /home/${cfgUser}/.dircolors
sudo -u ${cfgUser} sed -i 's/DIR 01;34/DIR 01;33/g' /home/${cfgUser}/.dircolors

# Squid 3 (prima di ubuntu 16 si chiamava squid3)
if [ "$cfgSquidPercent" != "" ]; then
	echo Setting up squid...
	apt-get -o Dpkg::Options::="--force-confnew" -y install squid3
	service squid stop
	# Max response size to accept (it discards bigger responses)
	# echo "reply_body_max_size 20 MB" >> /etc/squid/squid.conf
	# Remove "forwarded for" headers
	echo "forwarded_for delete" >> /etc/squid/squid.conf
	# Max cache RAM
	if [[ "$cfgSquidRam" != "" ]]; then
		echo "cache_mem $cfgSquidRam" >> /etc/squid/squid.conf
	fi
	rm -r /var/spool/squid/*
	freespacek=$( df --output=avail / | tail -1 )
	cachespacek=$((freespacek*cfgSquidPercent/100))
	cachespacem=$((cachespacek/1024))
	levels=$( perl -e "print int($((cachespacek/10))**(1/3))" )
	echo -e "cache_dir aufs /var/spool/squid ${cachespacem} ${levels} ${levels}\n" >> /etc/squid/squid.conf
	squid -z
	sleep 2
	echo Starting squid...
	service squid start
fi

chown ${cfgTomcatUser} ${projectBase}/contents ${projectBase}/logs
chown ${cfgUser} ${projectBase}/deploy
chown ${cfgUser} ${projectBase}/bin


# Other
if [[ $cfgPkgOther ]]; then
	apt-get -o Dpkg::Options::="--force-confnew" -y install $cfgPkgOther
fi

if [ -f $DIR/terraformingMore.sh ]; then
	echo terraformingMore...
	source $DIR/terraformingMore.sh
fi



