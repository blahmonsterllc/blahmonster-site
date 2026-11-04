import Foundation

@main
struct EmailDigestCommand {
    static func main() async {
        let options = Options.parse(Array(CommandLine.arguments.dropFirst()))

        if let problem = Triage.unavailableReason() {
            fail(problem)
        }

        logLine("Reading the last \(options.hoursBack) hours of mail from Apple Mail…")
        let mail: MailReader.Result
        do {
            mail = try MailReader.fetch(hoursBack: options.hoursBack, maxMessages: options.maxMessages)
        } catch {
            fail(error.localizedDescription)
        }

        let messages = mail.messages
        if messages.isEmpty {
            logLine("No new email found.")
        } else {
            logLine("Sorting \(messages.count) emails with Apple Intelligence (on this Mac)…")
        }

        var results: [TriagedEmail] = []
        for (index, message) in messages.enumerated() {
            let result = await Triage.assess(message)
            results.append(result)
            logLine("  [\(index + 1)/\(messages.count)] \(result.category.shortTitle): \(message.senderName) · \(message.subject)")
        }

        let overview = await Triage.overview(of: results)
        let digest = Digest(
            generatedAt: Date(),
            hoursBack: options.hoursBack,
            emails: results,
            overview: overview,
            totalInMail: mail.totalMatched
        )

        let reportURL: URL
        do {
            reportURL = try save(ReportWriter.html(for: digest), in: options.outputDirectory, date: digest.generatedAt)
        } catch {
            fail("Couldn't save the report: \(error.localizedDescription)")
        }
        logLine("Report saved to \(reportURL.path)")

        if options.notify {
            notify(digest)
        }
        if options.openReport {
            run("/usr/bin/open", [reportURL.path])
        }
    }

    /// Saves a dated copy plus `latest.html`, which always holds the newest report.
    static func save(_ html: String, in directory: URL, date: Date) throws -> URL {
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd-HHmm"
        let dated = directory.appendingPathComponent("digest-\(formatter.string(from: date)).html")
        try html.write(to: dated, atomically: true, encoding: .utf8)
        try html.write(to: directory.appendingPathComponent("latest.html"), atomically: true, encoding: .utf8)
        return dated
    }

    static func notify(_ digest: Digest) {
        let important = digest.count(.important)
        let message: String
        if digest.emails.isEmpty {
            message = "No new email."
        } else {
            message = "\(important) important · \(digest.count(.receipt)) receipts · \(digest.count(.promotion)) promotions · \(digest.count(.junk)) junk"
        }
        // The message only contains numbers and fixed words, so it's safe to put in the script.
        run("/usr/bin/osascript", ["-e", "display notification \"\(message)\" with title \"Email Digest\""])
    }

    @discardableResult
    static func run(_ executable: String, _ arguments: [String]) -> Int32 {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: executable)
        process.arguments = arguments
        do {
            try process.run()
            process.waitUntilExit()
            return process.terminationStatus
        } catch {
            return -1
        }
    }
}

struct Options {
    var hoursBack = 24
    var maxMessages = 150
    var outputDirectory = FileManager.default.homeDirectoryForCurrentUser
        .appendingPathComponent("Email Digests", isDirectory: true)
    var openReport = true
    var notify = true

    static let usage = """
    email-digest: a daily summary of your Apple Mail inbox, written by Apple Intelligence on this Mac.

    Usage: email-digest [options]

      --hours N      Look back N hours (default 24)
      --max N        Read at most N emails (default 150)
      --output DIR   Folder for the reports (default ~/Email Digests)
      --no-open      Don't open the report when it's done
      --no-notify    Don't show a notification when it's done
      -h, --help     Show this help
    """

    static func parse(_ args: [String]) -> Options {
        var options = Options()
        var index = 0

        func nextValue(for flag: String) -> String {
            index += 1
            guard index < args.count else { fail("\(flag) needs a value.\n\n\(usage)") }
            return args[index]
        }
        func positiveInt(for flag: String) -> Int {
            let raw = nextValue(for: flag)
            guard let value = Int(raw), value > 0 else { fail("\(flag) needs a positive number, not \"\(raw)\".") }
            return value
        }

        while index < args.count {
            switch args[index] {
            case "--hours":
                options.hoursBack = positiveInt(for: "--hours")
            case "--max":
                options.maxMessages = positiveInt(for: "--max")
            case "--output":
                let path = NSString(string: nextValue(for: "--output")).expandingTildeInPath
                options.outputDirectory = URL(fileURLWithPath: path, isDirectory: true)
            case "--no-open":
                options.openReport = false
            case "--no-notify":
                options.notify = false
            case "-h", "--help":
                print(usage)
                exit(0)
            default:
                fail("Unknown option \"\(args[index])\".\n\n\(usage)")
            }
            index += 1
        }
        return options
    }
}

func logLine(_ message: String) {
    print(message)
    fflush(stdout)
}

func fail(_ message: String) -> Never {
    FileHandle.standardError.write(Data("email-digest: \(message)\n".utf8))
    exit(1)
}
