#!/bin/bash

FILE="./inputdata"
if [ $# -ge 1 ]; then
  FILE=$1
fi
PATHFOLDER="repo"
if [ $# -ge 2 ]; then
  PATHFOLDEr=$2
fi

echo "Recupero Repository da file $FILE in $PATHFOLDER"

if [ ! -e $FILE ]
then
  echo "File indicato non accettabile"
  exit
fi
if [[ ! -r $FILE ]]
then
  echo "File indicato non leggibile"
  exit
fi

if [[ ! -d $PATHFOLDER ]]
then
  echo "Path inserita non accettabile"
  exit
fi

while read -r line
do
  if [[  ${line:0:1} !=  "#" ]]
  then
      if [[ ${line:0:5} != "http" ]]
      then
        if [[  -d "${PATHFOLDER}/${line:19}" ]]
        then
          cd "${PATHFOLDER}/${line:19}"
          git pull --force
          cd ..
        else
          git clone $line "${PATHFOLDER}/${line:19}"
        fi
      fi
  fi
done < "$FILE"
