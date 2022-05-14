#!/bin/bash

# The directory in which your application is installed
APPLICATION_DIR="."
# The Java command to use to launch the application (must be java 8+)
JAVA=java

# ***********************************************
OUT_FILE="${APPLICATION_DIR}"/out.log
RUNNING_PID="${APPLICATION_DIR}"/BokslCoin.pid
# ***********************************************

# colors
red='\e[0;31m'
green='\e[0;32m'
yellow='\e[0;33m'
reset='\e[0m'

echoRed() { echo -e "${red}$1${reset}"; }
echoGreen() { echo -e "${green}$1${reset}"; }
echoYellow() { echo -e "${yellow}$1${reset}"; }

# Check whether the application is running.
# The check is pretty simple: open a running pid file and check that the process
# is alive.
isrunning() {
  # Check for running app
  if [ -f "$RUNNING_PID" ]; then
    proc=$(cat $RUNNING_PID);
    if /bin/ps --pid $proc 1>&2 >/dev/null;
    then
      return 0
    fi
  fi
  return 1
}

start() {
  if isrunning; then
    echoYellow "The BokslCoin application is already running"
    return 0
  fi

  nohup $JAVA -Dlogging.config=../conf/logback-spring.xml -Dspring.profiles.active=local -jar ../lib/BokslCoin-0.6.0.jar --spring.config.location=file:../conf/BokslCoin.yml 1> /dev/null 2>&1 &
  echo $! > ${RUNNING_PID}

  if isrunning; then
    echoGreen "BokslCoin Application started"
    exit 0
  else
    echoRed "The BokslCoin Application has not started - check log"
    exit 3
  fi
}

console() {
  $JAVA -Dlogging.config=../conf/logback-spring.xml -Dspring.profiles.active=local -jar ../lib/BokslCoin-0.6.0.jar --spring.config.location=file:../conf/BokslCoin.yml
}

restart() {
  echo "Restarting BokslCoin Application"
  stop
  sleep 5
  start
}

stop() {
  echoYellow "Stopping BokslCoin Application"
  if isrunning; then
    kill -9 `cat $RUNNING_PID`
    rm $RUNNING_PID
  fi
}

status() {
  if isrunning; then
    echoGreen "BokslCoin Application is running"
  else
    echoRed "BokslCoin Application is either stopped or inaccessible"
  fi
}

case "$1" in
start)
    start
;;

console)
    console
;;

status)
   status
   exit 0
;;

stop)
    if isrunning; then
	stop
	exit 0
    else
	echoRed "Application not running"
	exit 3
    fi
;;

restart)
    stop
    start
;;

*)
    echo "Usage: $0 {status|start|console|stop|restart}"
    exit 1

esac
