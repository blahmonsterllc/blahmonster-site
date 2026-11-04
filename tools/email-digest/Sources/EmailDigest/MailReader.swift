import Foundation

/// Reads recent inbox messages from the Apple Mail app with AppleScript.
///
/// Going through Mail.app (rather than logging in to each mail server) means it
/// works with every account you've added to Mail (iCloud, Gmail, Outlook,
/// work), and no passwords are needed. The first run asks for permission to
/// control Mail.
enum MailReader {
    struct Result {
        let messages: [MailMessage]
        /// How many inbox messages matched before the `maxMessages` cap.
        let totalMatched: Int
    }

    enum ReadError: LocalizedError {
        case notAuthorized
        case scriptFailed(String)

        var errorDescription: String? {
            switch self {
            case .notAuthorized:
                return """
                macOS didn't allow this app to read Apple Mail.
                Open System Settings → Privacy & Security → Automation, find the app you ran \
                this from (Terminal, or email-digest), and turn on "Mail". Then run it again.
                """
            case .scriptFailed(let details):
                return "Couldn't read from Apple Mail: \(details)"
            }
        }
    }

    static let fieldSeparator: Character = "\u{1F}"
    static let recordSeparator: Character = "\u{1E}"

    static func fetch(hoursBack: Int, maxMessages: Int, maxBodyCharacters: Int = 4000) throws -> Result {
        let scriptURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("email-digest-\(UUID().uuidString).applescript")
        try appleScript.write(to: scriptURL, atomically: true, encoding: .utf8)
        defer { try? FileManager.default.removeItem(at: scriptURL) }

        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/usr/bin/osascript")
        process.arguments = [scriptURL.path, String(hoursBack), String(maxMessages), String(maxBodyCharacters)]
        let output = Pipe()
        let errors = Pipe()
        process.standardOutput = output
        process.standardError = errors
        try process.run()
        let outputData = output.fileHandleForReading.readDataToEndOfFile()
        let errorData = errors.fileHandleForReading.readDataToEndOfFile()
        process.waitUntilExit()

        guard process.terminationStatus == 0 else {
            let message = String(decoding: errorData, as: UTF8.self).trimmingCharacters(in: .whitespacesAndNewlines)
            // -1743 is "Not authorized to send Apple events".
            if message.contains("-1743") || message.localizedCaseInsensitiveContains("not authorized") {
                throw ReadError.notAuthorized
            }
            throw ReadError.scriptFailed(message.isEmpty ? "osascript exited with status \(process.terminationStatus)" : message)
        }
        return parse(String(decoding: outputData, as: UTF8.self))
    }

    /// Output format: `<total matched>` RS `<record>` RS `<record>` …
    /// Each record: id FS account FS sender FS subject FS secondsAgo FS isRead FS isJunk FS body
    static func parse(_ output: String, now: Date = Date()) -> Result {
        // osascript adds a trailing newline to whatever the script returns.
        var text = Substring(output)
        if text.hasSuffix("\n") { text = text.dropLast() }

        var records = text.split(separator: recordSeparator, omittingEmptySubsequences: false)
        guard !records.isEmpty else { return Result(messages: [], totalMatched: 0) }
        let total = Int(records.removeFirst().trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0

        let messages: [MailMessage] = records.compactMap { record in
            let fields = record.split(separator: fieldSeparator, omittingEmptySubsequences: false).map(String.init)
            guard fields.count >= 8 else { return nil }
            let secondsAgo = Double(fields[4].trimmingCharacters(in: .whitespaces)) ?? 0
            return MailMessage(
                messageID: fields[0],
                account: fields[1],
                sender: fields[2],
                subject: fields[3].isEmpty ? "(no subject)" : fields[3],
                received: now.addingTimeInterval(-secondsAgo),
                isRead: fields[5] == "true",
                markedJunkByMail: fields[6] == "true",
                body: fields[7...].joined(separator: " ")
            )
        }
        return Result(messages: messages, totalMatched: max(total, messages.count))
    }

    /// Arguments: hours to look back, max messages, max body characters per message.
    /// Dates are sent as "seconds ago" so the output doesn't depend on the Mac's
    /// date format or language.
    static let appleScript = #"""
    on run argv
        set hoursBack to (item 1 of argv) as integer
        set maxMessages to (item 2 of argv) as integer
        set maxChars to (item 3 of argv) as integer
        set nowDate to current date
        set cutoff to nowDate - (hoursBack * hours)
        set FS to character id 31
        set RS to character id 30
        set rowList to {}
        set total to 0

        tell application "Mail"
            try
                -- "inbox" is Mail's combined inbox across all accounts.
                set msgs to (every message of inbox whose date received > cutoff)
            on error
                -- Fallback: go through each account's inbox one by one.
                set msgs to {}
                repeat with acc in accounts
                    try
                        set msgs to msgs & (every message of mailbox "INBOX" of acc whose date received > cutoff)
                    end try
                end repeat
            end try
            set total to count of msgs
            set n to total
            if n > maxMessages then set n to maxMessages
            repeat with i from 1 to n
                try
                    set m to item i of msgs
                    set secsAgo to (nowDate - (date received of m)) as integer
                    set subj to my safeText(subject of m)
                    set frm to my safeText(sender of m)
                    set isRead to (read status of m) as text
                    set isJunk to (junk mail status of m) as text
                    set acct to ""
                    try
                        set acct to my safeText(name of account of mailbox of m)
                    end try
                    set msgId to ""
                    try
                        set msgId to my safeText(message id of m)
                    end try
                    set bodyText to ""
                    try
                        set bodyText to my clip(my safeText(content of m), maxChars)
                    end try
                    set end of rowList to msgId & FS & acct & FS & frm & FS & subj & FS & (secsAgo as text) & FS & isRead & FS & isJunk & FS & bodyText
                end try
            end repeat
        end tell

        set AppleScript's text item delimiters to RS
        set output to ((total as text) & RS & (rowList as text))
        set AppleScript's text item delimiters to ""
        return output
    end run

    -- Turns missing values into "" and removes our separator characters.
    on safeText(v)
        if v is missing value then return ""
        set t to v as text
        set AppleScript's text item delimiters to {character id 30, character id 31}
        set parts to text items of t
        set AppleScript's text item delimiters to " "
        set t to parts as text
        set AppleScript's text item delimiters to ""
        return t
    end safeText

    on clip(t, maxChars)
        if (length of t) > maxChars then return text 1 thru maxChars of t
        return t
    end clip
    """#
}
