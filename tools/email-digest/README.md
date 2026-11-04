# Email Digest

A daily summary of your Apple Mail inbox, written by **Apple Intelligence on your Mac**.

Every morning it reads the last 24 hours of email in the Mail app and sorts each message into:

- **Needs your attention:** real people, bills, appointments, deadlines, account security
- **Receipts & orders**
- **Updates & newsletters**
- **Promotions:** companies selling things, plus which senders send the most
- **Likely junk:** spam, scams, phishing

Each email gets a one-line summary and, where it makes sense, a to-do ("Pay $142.18 by Nov 20").
The report opens as a web page, and clicking a subject opens that email in Mail.
Open `sample-report.html` to see an example.

**Private by design:** it uses Apple's on-device model (the Foundation Models framework), so your
email never leaves your Mac. It only *reads* mail; it never moves, deletes or sends anything.

## What you need

- A Mac with Apple silicon (M1 or later) on **macOS 26 Tahoe** or later
- **Apple Intelligence turned on:** System Settings → Apple Intelligence & Siri
- **Xcode 26 or later** (free from the App Store). Open it once to finish setup, then run
  `sudo xcode-select -s /Applications/Xcode.app` in Terminal.
- Your email accounts added to the Mail app (iCloud, Gmail, Outlook, all work)

## Try it

```bash
cd tools/email-digest
swift run -c release email-digest
```

The first time, macOS asks whether Terminal may control Mail. Click **OK**.
The report is saved to `~/Email Digests/` and opens in your browser.

Options:

```
--hours N      Look back N hours (default 24)
--max N        Read at most N emails (default 150)
--output DIR   Folder for the reports (default ~/Email Digests)
--no-open      Don't open the report when it's done
--no-notify    Don't show a notification when it's done
```

Each email takes roughly a second or two, so 100 emails take a few minutes.

## Run it every morning

```bash
./install.sh            # every day at 7:30
./install.sh 06:45      # or pick a time (24-hour clock)
```

This builds the app, copies it to `~/.local/bin/email-digest`, and schedules it with macOS
(launchd). It runs once right away so macOS can ask for permission to control Mail; click **OK**.
If the Mac is asleep at the scheduled time, it runs when the Mac wakes.
`~/Email Digests/latest.html` always holds the newest report, so you can bookmark it.

To stop it: `./uninstall.sh` (your saved reports are kept).

## Troubleshooting

- **"macOS didn't allow this app to read Apple Mail":** System Settings → Privacy & Security →
  Automation → turn on **Mail** under Terminal (or under `email-digest` for the scheduled run).
- **"Apple Intelligence is turned off / still getting ready":** turn it on in System Settings,
  and give the model time to download.
- **Some emails land in "Couldn't sort":** Apple's on-device safety filter sometimes refuses
  spammy content, or an email is in an unsupported language. They're still listed so you can
  check them yourself.
- **Scheduled run didn't happen:** check `~/Library/Logs/email-digest.log`.

## How it works

| File | What it does |
| --- | --- |
| `Sources/EmailDigest/MailReader.swift` | Asks Mail (via AppleScript) for inbox messages from the last N hours |
| `Sources/EmailDigest/Triage.swift` | Sends each email to the on-device model and gets back a category, summary, to-do and priority (1–5) |
| `Sources/EmailDigest/ReportWriter.swift` | Builds the HTML report |
| `Sources/EmailDigest/EmailDigest.swift` | Command-line options, saving, notification |

To change how emails are sorted, edit the category descriptions in `Triage.instructions`.
The on-device model has a small context window (about 4,000 tokens), so each email is processed in
its own session, with links stripped and the body trimmed to 1,500 characters.
