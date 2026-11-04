#!/bin/bash
# Smoke test: install, open each screen, fail on crash. Prints small screenshots as base64 for review.
set -u
PKG=app.glint
adb install -r -g glint.apk || exit 1
adb logcat -c
# Emulator screens are small; work in fractions of the real size.
read W H < <(adb shell wm size | tail -1 | sed 's/.*: //; s/x/ /')
echo "screen ${W}x${H}"
X() { echo $(( W * $1 / 100 )); }
Y() { echo $(( H * $1 / 100 )); }
shot() {
  adb exec-out screencap -p > "/tmp/$1.png"
  convert "/tmp/$1.png" -resize 360x "/tmp/$1.jpg" 2>/dev/null || cp "/tmp/$1.png" "/tmp/$1.jpg"
  echo "SHOT-BEGIN $1"; base64 -w0 "/tmp/$1.jpg"; echo; echo "SHOT-END $1"
}
open() { adb shell am start -W -n "$1" ${2:-} >/dev/null; sleep 6; shot "$3"; }

open $PKG/.MainActivity "" settings
adb shell input tap $(X 50) $(Y 90) || true; sleep 3; shot settings_tap
open $PKG/app.glint.launcher.LauncherActivity "" home_bento
adb shell input swipe $(X 50) $(Y 70) $(X 50) $(Y 15) 400; sleep 3; shot drawer
adb shell input keyevent KEYCODE_BACK; sleep 1
adb shell input keyevent KEYCODE_HOME; sleep 2
adb shell settings put system accelerometer_rotation 1
open $PKG/app.glint.dock.DockActivity "" dock
adb shell input tap $(Y 50) $(X 50); sleep 3; shot dock_page2
adb shell input tap $(Y 50) $(X 50); sleep 3; shot dock_page3
adb shell input keyevent KEYCODE_BACK; sleep 2

echo "==== crashes ===="
if adb logcat -d -b crash | grep -E "FATAL EXCEPTION|AndroidRuntime" -A 30; then
  echo "SMOKE: CRASH FOUND"; exit 1
fi
adb logcat -d | grep -E " E (Glance|AppWidget|AndroidRuntime)" | tail -40
echo "SMOKE: OK"
