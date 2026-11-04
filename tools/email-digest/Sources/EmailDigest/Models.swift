import Foundation

/// One email read from Apple Mail.
struct MailMessage {
    let messageID: String
    let account: String
    let sender: String
    let subject: String
    let received: Date
    let isRead: Bool
    let markedJunkByMail: Bool
    let body: String

    /// "Jane Doe <jane@example.com>" -> "Jane Doe"; a bare address stays as is.
    var senderName: String {
        let trimmed = sender.trimmingCharacters(in: .whitespaces)
        if let angle = trimmed.firstIndex(of: "<") {
            let name = trimmed[..<angle]
                .trimmingCharacters(in: .whitespaces)
                .trimmingCharacters(in: CharacterSet(charactersIn: "\""))
            if !name.isEmpty { return name }
            return trimmed[angle...].trimmingCharacters(in: CharacterSet(charactersIn: "<> "))
        }
        return trimmed.isEmpty ? "Unknown sender" : trimmed
    }

    /// The body with links and extra whitespace removed, cut to `limit` characters.
    /// Links use up a lot of the model's small context window and say little.
    func cleanedBody(limit: Int) -> String {
        var text = body.replacingOccurrences(of: #"https?://\S+"#, with: "[link]", options: .regularExpression)
        text = text.replacingOccurrences(of: #"[ \t\x{00A0}]+"#, with: " ", options: .regularExpression)
        text = text.replacingOccurrences(of: #"\s*\n\s*"#, with: "\n", options: .regularExpression)
        text = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.count > limit {
            text = String(text.prefix(limit)) + "…"
        }
        return text
    }
}

/// The sections of the report, in the order they appear.
enum ReportCategory: Int, CaseIterable {
    case important
    case receipt
    case update
    case promotion
    case junk
    case unsorted

    var anchor: String {
        switch self {
        case .important: return "important"
        case .receipt: return "receipts"
        case .update: return "updates"
        case .promotion: return "promotions"
        case .junk: return "junk"
        case .unsorted: return "unsorted"
        }
    }

    var title: String {
        switch self {
        case .important: return "Needs your attention"
        case .receipt: return "Receipts & orders"
        case .update: return "Updates & newsletters"
        case .promotion: return "Promotions"
        case .junk: return "Likely junk"
        case .unsorted: return "Couldn't sort"
        }
    }

    var shortTitle: String {
        switch self {
        case .important: return "Important"
        case .receipt: return "Receipts"
        case .update: return "Updates"
        case .promotion: return "Promotions"
        case .junk: return "Junk"
        case .unsorted: return "Unsorted"
        }
    }

    var subtitle: String {
        switch self {
        case .important: return "From real people, or about money, dates, deadlines and your accounts."
        case .receipt: return "Order confirmations, shipping updates and payments."
        case .update: return "Newsletters, notifications and automated notices. Nothing to do."
        case .promotion: return "Companies trying to sell you something."
        case .junk: return "Spam, scams and phishing. Don't click links in these."
        case .unsorted: return "Apple Intelligence couldn't read these, so check them yourself."
        }
    }
}

/// An email after Apple Intelligence has sorted and summarized it.
struct TriagedEmail {
    let message: MailMessage
    let category: ReportCategory
    let summary: String
    let action: String
    /// 1 (ignore) to 5 (urgent). 0 when the email couldn't be analyzed.
    let priority: Int
    /// Why the email couldn't be analyzed, if it couldn't.
    let problem: String?
}

/// Everything the report needs.
struct Digest {
    let generatedAt: Date
    let hoursBack: Int
    let emails: [TriagedEmail]
    let overview: String?
    /// How many emails matched in Mail before the --max limit was applied.
    let totalInMail: Int

    func emails(in category: ReportCategory) -> [TriagedEmail] {
        emails
            .filter { $0.category == category }
            .sorted { a, b in
                if a.priority != b.priority { return a.priority > b.priority }
                return a.message.received > b.message.received
            }
    }

    func count(_ category: ReportCategory) -> Int {
        emails.reduce(0) { $0 + ($1.category == category ? 1 : 0) }
    }

    /// The companies sending the most promotions: handy for deciding what to unsubscribe from.
    func topSenders(in category: ReportCategory, limit: Int = 5) -> [(name: String, count: Int)] {
        var counts: [String: Int] = [:]
        for email in emails where email.category == category {
            counts[email.message.senderName, default: 0] += 1
        }
        return counts
            .filter { $0.value > 1 }
            .sorted { $0.value != $1.value ? $0.value > $1.value : $0.key < $1.key }
            .prefix(limit)
            .map { (name: $0.key, count: $0.value) }
    }
}
