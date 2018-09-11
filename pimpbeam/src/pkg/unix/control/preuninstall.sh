#!/bin/sh
set -e
echo -n "Executing preuninstall..."
APP_NAME=pimpbeam
if [ -f /etc/default/${APP_NAME} ] ; then
  . /etc/default/${APP_NAME}
fi
echo -n "Stopping ${APP_NAME}..."
stopreturn=`service ${APP_NAME} stop 2>/dev/null`
# Deletes user, if it exists
# echo is a hack so the exit value will be 0
user=`id -nu ${APP_USER} 2>/dev/null || echo ""`
if [ "${user}" = "${APP_USER}" ]; then
    echo -n "Deleting user ${APP_USER}..."
    userdel ${APP_USER}
fi
echo "Preuninstall done."

