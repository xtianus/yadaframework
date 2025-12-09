#!/bin/bash
# Fix Buildship generated Eclipse project
# Usage: fixProject projectFolder

projectFolder=$1

if [ "$projectFolder" = "" ]
then
	echo "Usage: fixProject <projectFolder>"
	exit 0;
fi

sed -i "s/include('lib')//g" $projectFolder/settings.gradle*

mv $projectFolder/lib/bin $projectFolder
mv $projectFolder/lib/src $projectFolder
mv $projectFolder/lib/.classpath $projectFolder
mv $projectFolder/lib/build.gradle $projectFolder
rm -rf $projectFolder/lib
rm -rf $projectFolder/src/main/java/*
rm -rf $projectFolder/src/test/java/*