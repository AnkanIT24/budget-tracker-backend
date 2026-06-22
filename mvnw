#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Start Up Batch script
# Required ENV vars:
# ------------------
#   JAVA_HOME - location of a JDK home dir
# ----------------------------------------------------------------------------

if [ -z "$JAVA_HOME" ] ; then
  if [ -r /etc/gentoo-release ] ; then
    JAVA_HOME=`java-config --jre-home`
  fi
fi

# resolve links - $0 may be a softlink
PRG="$0"
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done

PRGDIR=`dirname "$PRG"`
MAVEN_PROJECTBASEDIR=${MAVEN_BASEDIR:-"$PRGDIR"}

MAVEN_OPTS="$MAVEN_OPTS -Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR"

WRAPPER_JAR="$PRGDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

exec "$JAVA_HOME/bin/java" \
  $MAVEN_OPTS \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.home=${M2_HOME}" \
  $WRAPPER_LAUNCHER $MAVEN_CONFIG "$@"
