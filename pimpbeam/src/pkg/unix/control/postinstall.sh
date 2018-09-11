#!/bin/sh
set -e
echo -n "Executing postinstall..."
APP_NAME=pimpbeam
if [ -f /etc/default/${APP_NAME} ] ; then
  . /etc/default/${APP_NAME}
fi

# Creates user
user=`id -nu ${APP_USER} 2>/dev/null || echo ""`
if [ "${user}" = "${APP_USER}" ]; then
    echo -n "User already exists..."
else
    echo -n "Creating user ${APP_USER}..."
    useradd -s /bin/false ${APP_USER}
    if [ ! $? ]; then
        echo -n "Unable to create user"
        exit 666
    fi
fi

# Sets permissions
chown -R ${APP_USER}:${APP_USER} ${APP_HOME}

# Installs as service
# Use update-rc.d for debian/ubuntu else chkconfig
if [ -x /usr/sbin/update-rc.d ]; then
    echo -n "Adding as service with update-rc.d..."
    update-rc.d ${APP_NAME} defaults && serviceOK=true
else
    echo -n "Initializing service with chkconfig..."
    chkconfig --add ${APP_NAME} && chkconfig ${APP_NAME} on && chkconfig --list ${APP_NAME} && serviceOK=true
fi
if [ ! ${serviceOK} ]; then
    echo -n "Error adding service"
    exit 1
fi

echo "Installation complete. You can now use 'service ${APP_NAME} start/stop/restart/status'."

