#!/bin/bash
# Smoke test: install, open each screen, fail on crash. Prints small screenshots as base64 for review.
set -u
PKG=app.glint
adb shell wm size 720x1520
adb shell wm density 300
sleep 3
adb install -r -g glint.apk || exit 1
adb shell appwidget grantbind --package $PKG
adb logcat -c
# Emulator screens are small; work in fractions of the real size.
read W H < <(adb shell wm size | tail -1 | sed 's/.*: //; s/x/ /')
echo "screen ${W}x${H}"
X() { echo $(( W * $1 / 100 )); }
Y() { echo $(( H * $1 / 100 )); }
shot() {
  adb exec-out screencap -p > "/tmp/$1.png"
  python3 -c "from PIL import Image; i=Image.open('/tmp/$1.png').convert('RGB'); i.thumbnail((420,900)); i.save('/tmp/$1.jpg', quality=72)" || return 0
  echo "SHOT-BEGIN $1"; base64 -w 3000 "/tmp/$1.jpg"; echo "SHOT-END $1"
}
open() { adb shell am start -S -W -n "$1" ${2:-} >/dev/null; sleep 6; shot "$3"; }

# The app icon opens the launcher (Glint is a launcher first).
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null; sleep 6; shot home_first
# Add a few widgets the way the in-app gallery does.
for d in clock_big analog_classic battery_ring date_month weather_now; do
  adb shell am start -W -a app.glint.action.ADD_WIDGET --es design $d -p $PKG >/dev/null; sleep 4
  adb shell input keyevent KEYCODE_BACK; sleep 1
done
open $PKG/app.glint.launcher.LauncherActivity "" home_widgets
# Swiping over widgets scrolls them; swiping up from the dock opens all apps.
adb shell input swipe $(X 50) $(Y 75) $(X 50) $(Y 15) 400; sleep 2; shot home_scrolled
adb shell input swipe $(X 50) $(Y 94) $(X 50) $(Y 30) 400; sleep 3; shot drawer
adb shell input keyevent KEYCODE_BACK; sleep 2
# Back on home stays in Glint; long-press empty home for the menu.
adb shell input keyevent KEYCODE_BACK; sleep 2
adb shell input swipe $(X 75) $(Y 28) $(X 75) $(Y 28) 1200; sleep 3; shot home_menu
adb shell input keyevent KEYCODE_BACK; sleep 1
adb shell input keyevent KEYCODE_HOME; sleep 2
# Dock without Pro shows the paywall.
adb shell settings put system accelerometer_rotation 1
open $PKG/app.glint.dock.DockActivity "" dock_locked
adb shell input keyevent KEYCODE_BACK; sleep 2
open $PKG/.MainActivity "" settings

open $PKG/.MainActivity "--es screen Widgets" gallery_widgets
open $PKG/.MainActivity "--es screen Wallpapers" gallery_wallpapers
open $PKG/.MainActivity "--es screen Dock" dock_settings
for p in 0 1 2 3 4 5 6; do
  adb shell am start -W -n $PKG/app.glint.debug.WidgetTestActivity --ei page $p >/dev/null; sleep 12; shot widgets_$p
  adb shell input swipe $(X 50) $(Y 85) $(X 50) $(Y 10) 600; sleep 3; shot widgets_${p}b
  adb shell input keyevent KEYCODE_BACK; sleep 1
done

# Same widgets and home in the Ink and Liquid styles (tap the style cards in settings).
for st in "Ink 49" "Liquid 80"; do
  set -- $st
  adb shell am start -S -W -n $PKG/.MainActivity >/dev/null; sleep 4
  adb shell input tap $(X $2) $(Y 51); sleep 4
  adb shell am start -W -n $PKG/app.glint.debug.WidgetTestActivity --ei page 0 >/dev/null; sleep 10; shot style_$1_w0
  adb shell input keyevent KEYCODE_BACK; sleep 1
  adb shell am start -W -n $PKG/app.glint.debug.WidgetTestActivity --ei page 4 >/dev/null; sleep 10; shot style_$1_w4
  adb shell input keyevent KEYCODE_BACK; sleep 1
  open $PKG/app.glint.launcher.LauncherActivity "" style_$1_home
done
echo "==== crashes ===="
if adb logcat -d -b crash | grep -E "FATAL EXCEPTION|AndroidRuntime" -A 30; then
  echo "SMOKE: CRASH FOUND"; exit 1
fi
adb logcat -d | grep -E " E (Glance|AppWidget|AndroidRuntime)" | tail -40
echo "SMOKE: OK"
