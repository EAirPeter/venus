#!/bin/bash

set -e

version=$1
file=$2

if [ $# -lt 2 ]; then
	echo "Usage: $0 VERSION FILE"
	exit 1
fi

mvn install:install-file \
 -DgroupId=edu.berkeley.eecs.venus164 \
 -DartifactId=venus164 \
 -Dversion=$version \
 -Dfile=$file \
 -Dpackaging=jar \
 -DgeneratePom=true \
 -DlocalRepositoryPath=. \
 -DcreateChecksum=true

git add -A .

git commit -m "Release: $version"