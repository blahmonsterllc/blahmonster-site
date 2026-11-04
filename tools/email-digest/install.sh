#!/bin/bash
# Builds email-digest and schedules it to run every morning.
#
#   ./install.sh          run daily at 7:30
#   ./install.sh 06:45    run daily at 6:45 (24-hour time)
#
# Extra options for email-digest can follow the time, e.g.:
#   ./install.sh 07:30 --hours 36 --no-notify
set -euo pipefail

TIME="${1:-07:30}"
[[ $# -gt 0 ]] && shift
HOUR="${TIME%%:*}"
MINUTE="${TIME##*:}"
if ! [[ "$TIME" == *:* && "$HOUR" =~ ^[0-9]{1,2}$ && "$MINUTE" =~ ^[0-9]{2}$ ]] || (( 10#$HOUR > 23 || 10#$MINUTE > 59 )); then
  echo "Usage: ./install.sh [HH:MM] [email-digest options]   e.g. ./install.sh 07:30" >&2
  exit 1
fi

LABEL="com.blahmonster.email-digest"
HERE="$(cd "$(dirname "$0")" && pwd)"
BIN="$HOME/.local/bin/email-digest"
PLIST="$HOME/Library/LaunchAgents/$LABEL.plist"
LOG="$HOME/Library/Logs/email-digest.log"

echo "Building (this takes a minute the first time)…"
swift build -c release --package-path "$HERE"
mkdir -p "$(dirname "$BIN")" "$(dirname "$PLIST")" "$(dirname "$LOG")"
cp "$HERE/.build/release/email-digest" "$BIN"

xml_escape() { sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' <<<"$1"; }

ARGS_XML="    <string>$(xml_escape "$BIN")</string>"
for arg in "$@"; do
  ARGS_XML+=$'\n'"    <string>$(xml_escape "$arg")</string>"
done

cat > "$PLIST" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>$LABEL</string>
  <key>ProgramArguments</key>
  <array>
$ARGS_XML
  </array>
  <key>StartCalendarInterval</key>
  <dict>
    <key>Hour</key>
    <integer>$((10#$HOUR))</integer>
    <key>Minute</key>
    <integer>$((10#$MINUTE))</integer>
  </dict>
  <key>StandardOutPath</key>
  <string>$(xml_escape "$LOG")</string>
  <key>StandardErrorPath</key>
  <string>$(xml_escape "$LOG")</string>
</dict>
</plist>
EOF

DOMAIN="gui/$(id -u)"
launchctl bootout "$DOMAIN/$LABEL" 2>/dev/null || true
launchctl bootstrap "$DOMAIN" "$PLIST"

printf 'Scheduled: every day at %02d:%02d.\n' "$((10#$HOUR))" "$((10#$MINUTE))"
echo "Running it once now, so macOS can ask for permission to read Mail. Click OK when asked."
launchctl kickstart "$DOMAIN/$LABEL"
echo "Reports go to ~/Email Digests. Log: $LOG"
