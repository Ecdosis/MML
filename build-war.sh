#!/bin/bash
if [ ! -d mml ]; then
  mkdir mml
  if [ $? -ne 0 ] ; then
    echo "couldn't create mml directory"
    exit
  fi
fi
if [ ! -d mml/WEB-INF ]; then
  mkdir mml/WEB-INF
  if [ $? -ne 0 ] ; then
    echo "couldn't create mml/WEB-INF directory"
    exit
  fi
fi
if [ ! -d mml/static ]; then
  mkdir mml/static
  if [ $? -ne 0 ] ; then
    echo "couldn't create mml/static directory"
    exit
  fi
fi
if [ ! -d mml/WEB-INF/lib ]; then
  mkdir mml/WEB-INF/lib
  if [ $? -ne 0 ] ; then
    echo "couldn't create mml/WEB-INF/lib directory"
    exit
  fi
fi
rm -f mml/WEB-INF/lib/*.jar
cp dist/MML.jar mml/WEB-INF/lib/
cp lib/*.jar mml/WEB-INF/lib/
cp -r js mml/static/
cp web.xml mml/WEB-INF/
jar cf mml.war -C mml WEB-INF -C mml static
echo "NB: you MUST copy the contents of tomcat-bin to \$tomcat_home/bin"
