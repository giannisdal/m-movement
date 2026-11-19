#!/bin/bash
set -e

# Clean up stale lock from previous container restart
rm -f /tmp/.X99-lock /tmp/.X11-unix/X99

touch /root/.Xauthority
export XAUTHORITY=/root/.Xauthority
export DISPLAY=:99

Xvfb :99 -screen 0 1920x1080x24 &

# Wait until the display is actually ready
until xdpyinfo -display :99 >/dev/null 2>&1; do
    sleep 0.2
done

exec python main.py



