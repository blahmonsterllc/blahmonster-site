import Foundation

/// Turns a `Digest` into a single self-contained HTML page.
enum ReportWriter {
    static func html(for digest: Digest) -> String {
        let total = digest.emails.count
        let dateTitle = format(digest.generatedAt, template: "EEEEMMMMd")
        let shortDate = format(digest.generatedAt, template: "EEEMMMd")

        var body = ""
        body += """
        <header>
          <p class="eyebrow">Email Digest</p>
          <h1>\(esc(dateTitle))</h1>
          <p class="meta">\(total) email\(total == 1 ? "" : "s") from the last \(digest.hoursBack) hours · \
        sorted at \(esc(format(digest.generatedAt, template: "jmm"))) by Apple Intelligence on this Mac</p>
        </header>

        """

        if digest.totalInMail > total {
            body += """
            <p class="notice">Only \(total) of \(digest.totalInMail) emails were read. Run with <code>--max \(digest.totalInMail)</code> to include them all.</p>

            """
        }

        if total == 0 {
            body += "<p class=\"empty\">Nothing new arrived in the last \(digest.hoursBack) hours.</p>\n"
        } else {
            if let overview = digest.overview {
                body += "<section class=\"overview\"><p>\(esc(overview))</p></section>\n"
            }
            body += "<nav class=\"chips\">\n"
            for category in ReportCategory.allCases {
                let n = digest.count(category)
                if n == 0 && category == .unsorted { continue }
                body += "  <a class=\"chip \(category.anchor)\" href=\"#\(category.anchor)\"><b>\(n)</b> \(category.shortTitle)</a>\n"
            }
            body += "</nav>\n"

            for category in ReportCategory.allCases {
                body += section(for: category, in: digest)
            }
        }

        body += """
        <footer>Made on your Mac with Apple Intelligence. Your email never left your computer. \
        Click a subject to open that email in Mail.</footer>
        """

        return """
        <!doctype html>
        <html lang="en">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Email Digest · \(esc(shortDate))</title>
        <style>\(css)</style>
        </head>
        <body>
        <main>
        \(body)
        </main>
        </body>
        </html>
        """
    }

    // MARK: - Sections

    private static func section(for category: ReportCategory, in digest: Digest) -> String {
        let emails = digest.emails(in: category)
        if emails.isEmpty && category != .important { return "" }

        var html = """
        <section id="\(category.anchor)" class="group \(category.anchor)">
          <h2>\(esc(category.title)) <span class="count">\(emails.count)</span></h2>
          <p class="subtitle">\(esc(category.subtitle))</p>

        """

        if emails.isEmpty {
            html += "  <p class=\"empty\">Nothing needs you right now.</p>\n"
        }

        if category == .promotion {
            let top = digest.topSenders(in: .promotion)
            if !top.isEmpty {
                let list = top.map { "\(esc($0.name)) (\($0.count))" }.joined(separator: ", ")
                html += "  <p class=\"top-senders\">Most promotions from: \(list). Worth unsubscribing?</p>\n"
            }
        }

        let detailed = category == .important || category == .receipt
        html += detailed ? "  <div class=\"cards\">\n" : "  <ul class=\"rows\">\n"
        for email in emails {
            html += detailed ? card(for: email) : row(for: email)
        }
        html += detailed ? "  </div>\n" : "  </ul>\n"
        html += "</section>\n"
        return html
    }

    private static func card(for email: TriagedEmail) -> String {
        let m = email.message
        var html = "    <article class=\"card\(email.priority >= 4 ? " urgent" : "")\">\n"
        html += "      <div class=\"from\">\(unreadDot(m))<span class=\"sender\">\(esc(m.senderName))</span>"
        html += "<span class=\"time\">\(esc(timeLabel(m.received)))\(accountLabel(m))</span></div>\n"
        html += "      <h3>\(subjectLink(m))</h3>\n"
        if !email.summary.isEmpty {
            html += "      <p class=\"summary\">\(esc(email.summary))</p>\n"
        }
        if !email.action.isEmpty {
            html += "      <p class=\"action\">\(esc(email.action))</p>\n"
        }
        html += "    </article>\n"
        return html
    }

    private static func row(for email: TriagedEmail) -> String {
        let m = email.message
        var html = "    <li>\n"
        html += "      <div class=\"row-head\">\(unreadDot(m))<span class=\"sender\">\(esc(m.senderName))</span>"
        html += "<span class=\"time\">\(esc(timeLabel(m.received)))</span></div>\n"
        html += "      <div class=\"row-subject\">\(subjectLink(m))</div>\n"
        if let problem = email.problem {
            html += "      <div class=\"row-summary\">\(esc(problem))</div>\n"
        } else if !email.summary.isEmpty {
            html += "      <div class=\"row-summary\">\(esc(email.summary))</div>\n"
        }
        html += "    </li>\n"
        return html
    }

    // MARK: - Pieces

    private static func unreadDot(_ m: MailMessage) -> String {
        m.isRead ? "" : "<span class=\"dot\" title=\"Unread\"></span>"
    }

    private static func accountLabel(_ m: MailMessage) -> String {
        m.account.isEmpty ? "" : " · \(esc(m.account))"
    }

    /// The subject, linked to the email in Apple Mail when we know its Message-ID.
    private static func subjectLink(_ m: MailMessage) -> String {
        guard let url = mailURL(forMessageID: m.messageID) else { return esc(m.subject) }
        return "<a href=\"\(esc(url))\">\(esc(m.subject))</a>"
    }

    /// `message://` links open a specific email in Apple Mail.
    static func mailURL(forMessageID id: String) -> String? {
        let bare = id.trimmingCharacters(in: CharacterSet(charactersIn: "<> \n\t"))
        guard !bare.isEmpty else { return nil }
        var allowed = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
        allowed.insert(charactersIn: "-._~@")
        guard let encoded = bare.addingPercentEncoding(withAllowedCharacters: allowed) else { return nil }
        return "message://%3C\(encoded)%3E"
    }

    private static func timeLabel(_ date: Date) -> String {
        if Calendar.current.isDateInToday(date) {
            return format(date, template: "jmm")
        }
        return format(date, template: "EEEjmm")
    }

    private static func format(_ date: Date, template: String) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale.current
        formatter.setLocalizedDateFormatFromTemplate(template)
        return formatter.string(from: date)
    }

    static func esc(_ text: String) -> String {
        text
            .replacingOccurrences(of: "&", with: "&amp;")
            .replacingOccurrences(of: "<", with: "&lt;")
            .replacingOccurrences(of: ">", with: "&gt;")
            .replacingOccurrences(of: "\"", with: "&quot;")
    }

    // MARK: - Style

    private static let css = """
    :root {
      color-scheme: light dark;
      --bg: #f6f5f2; --surface: #ffffff; --text: #1d1d1f; --muted: #6e6e73; --line: #e3e1dc;
      --accent: #0a66d8; --important: #d93a2b; --receipt: #2f8f5b; --update: #6b6f80;
      --promotion: #b7791f; --junk: #8e44ad; --unsorted: #6e6e73; --urgent-bg: #fff4f2;
    }
    @media (prefers-color-scheme: dark) {
      :root {
        --bg: #161618; --surface: #1f1f22; --text: #f2f2f4; --muted: #a1a1a8; --line: #33333a;
        --accent: #5ea2ff; --important: #ff6b5c; --receipt: #4cc38a; --update: #a0a4b8;
        --promotion: #e5a74a; --junk: #c38ce0; --unsorted: #a1a1a8; --urgent-bg: #2a1d1b;
      }
    }
    * { box-sizing: border-box; }
    body { margin: 0; background: var(--bg); color: var(--text);
      font: 16px/1.5 -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif; }
    main { max-width: 760px; margin: 0 auto; padding: 40px 16px 64px; }
    a { color: inherit; text-decoration-color: var(--line); text-underline-offset: 3px; }
    a:hover { color: var(--accent); text-decoration-color: currentColor; }
    header { margin-bottom: 24px; }
    .eyebrow { margin: 0; color: var(--muted); font-size: 13px; font-weight: 600; letter-spacing: .06em; text-transform: uppercase; }
    h1 { margin: 4px 0 6px; font-size: 34px; line-height: 1.15; letter-spacing: -.02em; }
    .meta, .subtitle, footer { color: var(--muted); font-size: 14px; }
    .meta { margin: 0; }
    .notice { background: var(--surface); border: 1px solid var(--line); border-radius: 10px; padding: 10px 14px; font-size: 14px; }
    .overview { background: var(--surface); border: 1px solid var(--line); border-radius: 14px; padding: 18px 20px; margin-bottom: 18px; }
    .overview p { margin: 0; font-size: 17px; }
    .chips { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 32px; }
    .chip { display: inline-flex; gap: 6px; align-items: baseline; padding: 6px 12px; border-radius: 999px;
      background: var(--surface); border: 1px solid var(--line); text-decoration: none; font-size: 14px; }
    .chip b { color: var(--c); }
    .important { --c: var(--important); } .receipts { --c: var(--receipt); } .updates { --c: var(--update); }
    .promotions { --c: var(--promotion); } .junk { --c: var(--junk); } .unsorted { --c: var(--unsorted); }
    .group { margin-top: 36px; scroll-margin-top: 16px; }
    h2 { display: flex; align-items: center; gap: 10px; margin: 0; font-size: 21px; }
    h2::before { content: ""; width: 10px; height: 10px; border-radius: 50%; background: var(--c); }
    .count { color: var(--muted); font-weight: 500; font-size: 16px; }
    .subtitle { margin: 2px 0 14px; }
    .top-senders { font-size: 14px; margin: -6px 0 14px; }
    .cards { display: grid; gap: 10px; }
    .card { background: var(--surface); border: 1px solid var(--line); border-left: 4px solid var(--c); border-radius: 12px; padding: 14px 16px; }
    .card.urgent { background: var(--urgent-bg); }
    .from, .row-head { display: flex; align-items: center; gap: 8px; font-size: 14px; min-width: 0; }
    .sender { font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .time { margin-left: auto; color: var(--muted); white-space: nowrap; font-size: 13px; }
    .dot { flex: none; width: 8px; height: 8px; border-radius: 50%; background: var(--accent); }
    h3 { margin: 4px 0 6px; font-size: 17px; line-height: 1.3; overflow-wrap: anywhere; }
    .summary { margin: 0; }
    .action { margin: 8px 0 0; font-weight: 600; color: var(--c); }
    .action::before { content: "→ "; }
    .rows { list-style: none; margin: 0; padding: 0; background: var(--surface); border: 1px solid var(--line); border-radius: 12px; }
    .rows li { padding: 10px 16px; border-top: 1px solid var(--line); }
    .rows li:first-child { border-top: 0; }
    .row-subject { font-size: 15px; overflow-wrap: anywhere; }
    .row-summary { color: var(--muted); font-size: 14px; }
    .empty { color: var(--muted); }
    code { font: 13px ui-monospace, SFMono-Regular, Menlo, monospace; }
    footer { margin-top: 48px; padding-top: 16px; border-top: 1px solid var(--line); }
    """
}
