#!/bin/bash
# Stops the daily email digest and removes the installed copy.
# Your saved reports in ~/Email Digests are left alone.
set -euo pipefail
LABEL="com.blahmonster.email-digest"
launchctl bootout "gui/$(id -u)/$LABEL" 2>/dev/null || true
rm -f "$HOME/Library/LaunchAgents/$LABEL.plist" "$HOME/.local/bin/email-digest"
echo "Daily email digest removed."
