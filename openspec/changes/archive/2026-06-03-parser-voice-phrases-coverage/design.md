## Context

The ReminderParser uses a candidate-based engine: regexes find ALL matches → candidates with scores → best date + best time → body = original minus spans. Current coverage is strong for explicit dates/times but weak on colloquialisms, compound expressions, and STT artifacts (nominative months, filler words).

## Goals / Non-Goals

**Goals:**
- Parse "next weekday" phrases ("в следующий понедельник")
- Parse weekend phrases ("на выходных")
- Parse compound Russian time expressions ("половина девятого", "без четверти девять")
- Parse approximate relative time ("через пару часов", "через неделю")
- Parse digit + "числа" ("5 числа", "9 числа")
- Handle STT nominative month output ("9 май" alongside "9 мая")
- Handle part-of-day prefix ("утром в 9", "вечером в 8")
- Reduce NO_TIME_FOUND rate for common colloquial phrases

**Non-Goals:**
- Multi-sentence parsing (one phrase = one reminder)
- Recurring reminders ("каждый день")
- Named holidays ("на Новый год")
- Duration parsing ("на час")
- Natural language dates with fuzzy references ("в конце недели")
- Changing the candidate-based engine architecture

## Decisions

### 1. Extend regex approach (not ML/LLM fallback)
**Rationale**: These are all rule-based patterns with deterministic resolution. Adding regexes is cheap, unit-testable, and offline-friendly. LLM fallback (фаза 5) remains for truly ambiguous input.

### 2. "Следующий" = +7 days for weekday, not just nearest
**Rationale**: "в следующий понедельник" unambiguously means the Monday of next week, even if today is Sunday. Simple `plusDays(7)` from nearest weekday.

### 3. Approximate relative time uses fixed defaults
**Rationale**: "через пару часов" → 2h, "через несколько минут" → 5min, "через неделю" → 7d. This is consistent with user intent and avoids over-engineering fuzzy parsing.

### 4. Compound time expressions use standard Russian conventions
**Rationale**: "половина девятого" = 8:30, "без пятнадцати девять" = 8:45, "четверть девятого" = 8:15. These are well-defined in Russian.

### 5. Nominative months matched alongside genitive
**Rationale**: STT may return nominative ("9 май" instead of "9 мая"). We'll add a parallel `monthFromNominative()` mapping without removing genitive support.

### 6. Digit ordinals reuse ordinalDayFromGroup logic
**Rationale**: "9 числа" is a digit variant of "девятого числа". We'll parse the digit, apply the same month-rollover logic, and use a shared helper.

## Risks / Trade-offs

- **[Risk]** More regexes increase maintenance burden and potential for false positives.
  - **Mitigation**: Strict word boundaries (`WB`/`WE`), score-based resolution, and extensive unit tests.
- **[Risk]** "Следующий" weekday may conflict with existing weekday regex if not ordered correctly.
  - **Mitigation**: "следующий" pattern checked BEFORE plain weekday; higher score ensures precedence.
- **[Risk]** Compound time expressions might conflict with existing patterns (e.g., "в девять" + "с половиной").
  - **Mitigation**: Compound regex eats the full phrase; score higher than simple hour patterns.
