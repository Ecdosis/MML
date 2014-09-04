#!/bin/bash
getjarpaths()
{
  JARPATH=""
  for f in $1/*.jar
  do
    JARPATH="$JARPATH:$f"
  done
  echo $JARPATH
  return
}
if [ "`uname`" = "Darwin" ]; then
  WEB_ROOT="/Library/WebServer/Documents"
else
  WEB_ROOT="/var/www"
fi
JARPATHS=`getjarpaths lib`
LIBPATH=`java LibPath`
if [[ $LIBPATH != */usr/local/lib* ]]
then
  LIBPATH=$LIBPATH:/usr/local/lib
fi
if [ `uname` = "Darwin" ]; then
  pgrep(){ ps -ax -o pid,command | grep "$@" | grep -v 'grep' | awk '{print $1;}'; }
  HPID=`pgrep MML.jar`
  if [ -n "$HPID" ]; then
    kill $HPID
  fi
else
  pkill -f MML.jar
fi
nohup java -Xss8m -Xmx2048m -Xincgc -Djava.library.path=$LIBPATH -cp .$JARPATHS:MML.jar mml.JettyServer -u admin -p jabberw0cky -r MONGO -d 27017 &

