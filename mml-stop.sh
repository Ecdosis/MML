#!/bin/sh
if [ `uname` = "Darwin" ]; then
  pgrep(){ ps -x -o pid,command | grep "$@" | grep -v 'grep' | awk '{print $1;}'; }
  HPID=`pgrep MML.jar`
  if [ -n "$HPID" ]; then
    kill $HPID
  fi
else
  pkill -f MML.jar
fi
