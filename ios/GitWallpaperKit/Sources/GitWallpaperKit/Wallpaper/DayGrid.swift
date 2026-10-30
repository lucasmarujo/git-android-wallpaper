public enum DayState: Sendable {
    case past
    case today
    case future
}

public struct DayCell: Equatable, Sendable {
    public var date: LocalDate
    public var level: ContributionLevel
    public var state: DayState
}

public struct DayGrid: Equatable, Sendable {
    public var startDate: LocalDate
    public var today: LocalDate
    public var cells: [DayCell]

    public static func from(_ days: [ContributionDay], period: ContributionPeriod, today: LocalDate) -> DayGrid {
        let lastReportedDate = days.map(\.date).max()
        let effectiveToday = lastReportedDate.map { max($0, today) } ?? today
        let startDate = switch period {
        case .currentYear: effectiveToday.firstDayOfYear
        case .last12Months: effectiveToday.minusYears(1).plusDays(1)
        }
        let endDate = switch period {
        case .currentYear: effectiveToday.lastDayOfYear
        case .last12Months: effectiveToday
        }
        let levelsByDate = Dictionary(days.map { ($0.date, $0.level) }, uniquingKeysWith: { _, last in last })
        let dayCount = startDate.daysUntil(endDate) + 1
        let cells = (0..<dayCount).map { offset in
            let date = startDate.plusDays(offset)
            let state: DayState = if date == effectiveToday {
                .today
            } else if date > effectiveToday {
                .future
            } else {
                .past
            }
            return DayCell(date: date, level: levelsByDate[date] ?? .empty, state: state)
        }
        return DayGrid(startDate: startDate, today: effectiveToday, cells: cells)
    }
}
