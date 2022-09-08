#!/bin/bash
#
# Zip some files and renames them in the archive
# Parameters: outputZip, zipnoteFile, sourceFiles

if (( $# < 3 )); then
        echo "Usage: $( basename $0 ) <outputZip> <zipnoteFile> <sourceFiles>"
        echo Example: $( basename $0 ) my.zip renames.txt file1 file2
        exit 1
fi

outputZip=$1
zipnoteFile=$2
shift 2
sourceFiles=$@

zipOptions="-9 --junk-paths --quiet"

zip $zipOptions $outputZip $sourceFiles

zipnote -w $outputZip < $zipnoteFile

